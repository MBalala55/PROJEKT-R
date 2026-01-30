package com.example.elektropregled.data.repository

import android.content.Context
import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.PostrojenjeEntity
import com.example.elektropregled.util.NetworkUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class PostrojenjeRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage,
    private val context: Context,
    private val checklistRepository: com.example.elektropregled.data.repository.ChecklistRepository? = null
) {
    
    private val apiService = ApiClient.apiService
    private val postrojenjeDao = database.postrojenjeDao()
    private val pregledDao = database.pregledDao()
    private val poljeDao = database.poljeDao()
    private val uredajDao = database.uredajDao()
    
    // Background scope for comprehensive sync (using SupervisorJob to prevent cancellation propagation)
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Get postrojenja (facilities) as Flow - OFFLINE-FIRST.
     * Returns data from local DB immediately, syncs with server in background when online.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPostrojenjaFlow(): Flow<List<PostrojenjeSummary>> {
        android.util.Log.d("PostrojenjeRepository", "getPostrojenjaFlow: Starting Flow")
        // Return Flow from local DB - this is the single source of truth
        return postrojenjeDao.getAllPostrojenja()
            .flowOn(Dispatchers.IO) // Ensure DB queries run on IO thread
            .catch { e ->
                android.util.Log.e("PostrojenjeRepository", "Error in getAllPostrojenja Flow", e)
                e.printStackTrace()
                // Emit empty list of entities - flatMapLatest will transform it to empty summaries
                emit(emptyList<PostrojenjeEntity>())
            }
            .flatMapLatest { entities ->
                android.util.Log.d("PostrojenjeRepository", "getPostrojenjaFlow: Received ${entities.size} entities from DB")
                // Use flow builder to properly handle suspend functions
                flow {
                    try {
                        // Build summaries list using suspend functions
                        val summaries = mutableListOf<PostrojenjeSummary>()
                        
                        // Process entities with proper error handling
                        if (entities.isEmpty()) {
                            android.util.Log.d("PostrojenjeRepository", "getPostrojenjaFlow: No entities found, emitting empty list")
                            emit(emptyList())
                            return@flow
                        }
                        
                        for (entity in entities) {
                            try {
                                // Get inspection stats from local DB (these are suspend functions)
                                val totalPregleda = try {
                                    pregledDao.getPregledCountByPostrojenje(entity.id_postr)
                                } catch (e: Exception) {
                                    android.util.Log.w("PostrojenjeRepository", "Error getting pregled count for ${entity.id_postr}", e)
                                    0
                                }
                                
                                val lastPregled = try {
                                    pregledDao.getLastPregledByPostrojenje(entity.id_postr)
                                } catch (e: Exception) {
                                    android.util.Log.w("PostrojenjeRepository", "Error getting last pregled for ${entity.id_postr}", e)
                                    null
                                }
                                
                                summaries.add(
                                    PostrojenjeSummary(
                                        idPostr = entity.id_postr,
                                        nazPostr = entity.naz_postr,
                                        lokacija = entity.lokacija,
                                        oznVrPostr = entity.ozn_vr_postr,
                                        totalPregleda = totalPregleda,
                                        zadnjiPregled = lastPregled?.pocetak,
                                        zadnjiKorisnik = null // Would need to join with Korisnik table
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("PostrojenjeRepository", "Error processing entity ${entity.id_postr}", e)
                                e.printStackTrace()
                                // Add entity without stats if there's an error
                                summaries.add(
                                    PostrojenjeSummary(
                                        idPostr = entity.id_postr,
                                        nazPostr = entity.naz_postr,
                                        lokacija = entity.lokacija,
                                        oznVrPostr = entity.ozn_vr_postr,
                                        totalPregleda = 0,
                                        zadnjiPregled = null,
                                        zadnjiKorisnik = null
                                    )
                                )
                            }
                        }
                        android.util.Log.d("PostrojenjeRepository", "getPostrojenjaFlow: Emitting ${summaries.size} summaries")
                        emit(summaries)
                    } catch (e: Exception) {
                        android.util.Log.e("PostrojenjeRepository", "Error in flow builder", e)
                        e.printStackTrace()
                        // Emit empty list on error so UI can show error state
                        emit(emptyList())
                    }
                }
            }
            .flowOn(Dispatchers.IO) // Ensure mapping with suspend calls runs on IO thread
            .catch { e ->
                android.util.Log.e("PostrojenjeRepository", "Error in flatMapLatest", e)
                e.printStackTrace()
                // Emit empty list on error so UI can show error state
                emit(emptyList<PostrojenjeSummary>())
            }
    }
    
    /**
     * Sync postrojenja from server (background operation).
     * Call this when online to update local database.
     */
    suspend fun syncPostrojenja(): Result<Unit> {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            return Result.failure(Exception("Nema internetske veze"))
        }
        
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
        }
        
        return try {
            val response = apiService.getPostrojenja("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val facilities = response.body()!!
                
                // Save to local database
                val entities = facilities.map { summary ->
                    PostrojenjeEntity(
                        id_postr = summary.idPostr,
                        ozn_vr_postr = summary.oznVrPostr,
                        naz_postr = summary.nazPostr,
                        lokacija = summary.lokacija
                    )
                }
                android.util.Log.d("PostrojenjeRepository", "Saving ${entities.size} postrojenja to local DB")
                try {
                    // Ensure we're on IO thread for database operations
                    withContext(Dispatchers.IO) {
                        postrojenjeDao.insertAll(entities)
                        android.util.Log.d("PostrojenjeRepository", "Postrojenja saved successfully")
                        
                        // Verify they were saved
                        val savedCount = postrojenjeDao.getAllPostrojenja().first().size
                        android.util.Log.d("PostrojenjeRepository", "Verified: ${savedCount} postrojenja in local DB")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PostrojenjeRepository", "Error saving postrojenja", e)
                    e.printStackTrace()
                    throw e
                }
                
                // Trigger comprehensive sync in background (non-blocking)
                // This will sync all polja and checklist data for all postrojenja in the background
                // Note: This is a best-effort sync. If it fails, the app will still work offline with seed data.
                if (checklistRepository != null) {
                    backgroundScope.launch {
                        android.util.Log.d("PostrojenjeRepository", "Starting background comprehensive sync for ${entities.size} postrojenja...")
                        var syncSuccessCount = 0
                        var syncErrorCount = 0
                        
                        // Check network and token once at the start
                        val isNetworkAvailable = NetworkUtil.isNetworkAvailable(context)
                        val token = tokenStorage.getToken()
                        val isTokenValid = token != null && tokenStorage.isTokenValid()
                        
                        android.util.Log.d("PostrojenjeRepository", "Background sync initial check - Network: $isNetworkAvailable, Token valid: $isTokenValid")
                        
                        if (!isNetworkAvailable || !isTokenValid) {
                            android.util.Log.w("PostrojenjeRepository", "Skipping comprehensive sync - Network: $isNetworkAvailable, Token valid: $isTokenValid")
                            return@launch
                        }
                        
                        for (entity in entities) {
                            try {
                                // Check network and token before each sync (they might change during long-running sync)
                                if (!NetworkUtil.isNetworkAvailable(context)) {
                                    android.util.Log.w("PostrojenjeRepository", "Network lost during sync, stopping comprehensive sync")
                                    break
                                }
                                
                                if (!tokenStorage.isTokenValid()) {
                                    android.util.Log.w("PostrojenjeRepository", "Token expired during sync, stopping comprehensive sync")
                                    break
                                }
                                
                                // Sync polja first
                                val poljaResult = syncPolja(entity.id_postr)
                                if (poljaResult.isSuccess) {
                                    // Then sync all checklist data for this postrojenje
                                    val checklistResult = checklistRepository.syncAllChecklistForPostrojenje(entity.id_postr)
                                    if (checklistResult.isSuccess) {
                                        syncSuccessCount++
                                        android.util.Log.d("PostrojenjeRepository", "Successfully synced all data for postrojenje ${entity.id_postr}")
                                    } else {
                                        syncErrorCount++
                                        android.util.Log.w("PostrojenjeRepository", "Failed to sync checklist for postrojenje ${entity.id_postr}: ${checklistResult.exceptionOrNull()?.message}")
                                    }
                                } else {
                                    syncErrorCount++
                                    android.util.Log.w("PostrojenjeRepository", "Failed to sync polja for postrojenje ${entity.id_postr}: ${poljaResult.exceptionOrNull()?.message}")
                                }
                                
                                // Small delay to avoid overwhelming the network
                                delay(100)
                            } catch (e: Exception) {
                                syncErrorCount++
                                android.util.Log.e("PostrojenjeRepository", "Error syncing postrojenje ${entity.id_postr}", e)
                            }
                        }
                        
                        android.util.Log.d("PostrojenjeRepository", "Background comprehensive sync completed: $syncSuccessCount successful, $syncErrorCount failed")
                    }
                } else {
                    android.util.Log.w("PostrojenjeRepository", "ChecklistRepository not available - skipping comprehensive sync")
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Greška pri učitavanju postrojenja: ${response.code()}"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Server timeout - provjerite internet konekciju", e))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Nemogućo se povezati - server je možda odsutan", e))
        } catch (e: Exception) {
            Result.failure(Exception("Greška pri učitavanju postrojenja: ${e.message}", e))
        }
    }
    
    /**
     * Trigger background sync if online (non-blocking, fails silently if offline).
     */
    fun triggerSyncIfOnline(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isNetworkAvailable(context)) {
                    syncPostrojenja()
                }
            } catch (e: Exception) {
                // Silently fail - app works offline with local data
                android.util.Log.d("PostrojenjeRepository", "Background sync failed (offline?): ${e.message}")
            }
        }
    }
    
    /**
     * Trigger background sync for polja if online (non-blocking, fails silently if offline).
     */
    fun triggerSyncPoljaIfOnline(scope: CoroutineScope, postrojenjeId: Int) {
        scope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isNetworkAvailable(context)) {
                    syncPolja(postrojenjeId)
                }
            } catch (e: Exception) {
                // Silently fail - app works offline with local data
                android.util.Log.d("PostrojenjeRepository", "Background sync polja failed (offline?): ${e.message}")
            }
        }
    }
    
    /**
     * Get polja (fields) as Flow - OFFLINE-FIRST.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPoljaFlow(postrojenjeId: Int): Flow<List<PoljeDto>> {
        return poljeDao.getPoljaByPostrojenje(postrojenjeId)
            .flowOn(Dispatchers.IO)
            .catch { e ->
                android.util.Log.e("PostrojenjeRepository", "Error in getPoljaByPostrojenje Flow", e)
                e.printStackTrace()
                emit(emptyList())
            }
            .flatMapLatest { entities ->
                flow {
                    try {
                        android.util.Log.d("PostrojenjeRepository", "getPoljaFlow: Processing ${entities.size} polja entities")
                        
                        val polja = mutableListOf<PoljeDto>()
                        
                        // Build polja from entities
                        for (entity in entities) {
                            try {
                                // Get device count for this field
                                val brojUredaja = try {
                                    uredajDao.getUredajCountByPolje(entity.id_polje)
                                } catch (e: Exception) {
                                    android.util.Log.w("PostrojenjeRepository", "Error getting device count for polje ${entity.id_polje}", e)
                                    0
                                }
                                
                                polja.add(
                                    PoljeDto(
                                        idPolje = entity.id_polje,
                                        napRazina = entity.nap_razina,
                                        oznVrPolje = entity.ozn_vr_polje,
                                        nazPolje = entity.naz_polje,
                                        brojUredaja = brojUredaja
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("PostrojenjeRepository", "Error processing polje entity ${entity.id_polje}", e)
                                e.printStackTrace()
                            }
                        }
                        
                        // Add virtual "Direktno na postrojenju" field if there are devices directly on facility
                        try {
                            val direktnoUredaji = uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
                            if (direktnoUredaji.isNotEmpty()) {
                                polja.add(0, PoljeDto(
                                    idPolje = null,
                                    napRazina = null,
                                    oznVrPolje = null,
                                    nazPolje = context.getString(com.example.elektropregled.R.string.directly_on_facility),
                                    brojUredaja = direktnoUredaji.size
                                ))
                                android.util.Log.d("PostrojenjeRepository", "getPoljaFlow: Added virtual field with ${direktnoUredaji.size} devices")
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("PostrojenjeRepository", "Error getting devices directly on facility", e)
                        }
                        
                        android.util.Log.d("PostrojenjeRepository", "getPoljaFlow: Emitting ${polja.size} polja")
                        emit(polja)
                    } catch (e: Exception) {
                        android.util.Log.e("PostrojenjeRepository", "Error in getPoljaFlow flow builder", e)
                        e.printStackTrace()
                        emit(emptyList())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                android.util.Log.e("PostrojenjeRepository", "Error in getPoljaFlow flatMapLatest", e)
                e.printStackTrace()
                emit(emptyList())
            }
    }
    
    /**
     * Get polja (backward compatibility) - OFFLINE-FIRST.
     * Always returns data from local DB, never requires network.
     */
    suspend fun getPolja(postrojenjeId: Int): Result<List<PoljeDto>> {
        return try {
            // Get from local DB - this is the single source of truth for offline mode
            val entities = poljeDao.getPoljaByPostrojenje(postrojenjeId).first()
            
            android.util.Log.d("PostrojenjeRepository", "getPolja: Found ${entities.size} polja in local DB for postrojenje $postrojenjeId")
            
            // Always build from local data, even if empty
            // If empty, check for devices directly on facility
            if (entities.isEmpty()) {
                android.util.Log.d("PostrojenjeRepository", "No polja entities, checking for devices directly on facility")
                val direktnoUredaji = uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
                if (direktnoUredaji.isNotEmpty()) {
                    android.util.Log.d("PostrojenjeRepository", "Found ${direktnoUredaji.size} devices directly on facility")
                    Result.success(listOf(PoljeDto(
                        idPolje = null,
                        napRazina = null,
                        oznVrPolje = null,
                        nazPolje = context.getString(com.example.elektropregled.R.string.directly_on_facility),
                        brojUredaja = direktnoUredaji.size
                    )))
                } else {
                    android.util.Log.d("PostrojenjeRepository", "No polja and no devices directly on facility - returning empty list")
                    Result.success(emptyList())
                }
            } else {
                buildPoljaDtoFromLocal(entities, postrojenjeId)
            }
        } catch (e: Exception) {
            android.util.Log.e("PostrojenjeRepository", "Error getting polja from local DB", e)
            Result.failure(Exception("Greška pri učitavanju polja: ${e.message}", e))
        }
    }
    
    private suspend fun buildPoljaDtoFromLocal(entities: List<com.example.elektropregled.data.database.entity.PoljeEntity>, postrojenjeId: Int): Result<List<PoljeDto>> {
        val polja = entities.map { entity ->
            // Get device count for this field
            val brojUredaja = uredajDao.getUredajCountByPolje(entity.id_polje)
            
            PoljeDto(
                idPolje = entity.id_polje,
                napRazina = entity.nap_razina,
                oznVrPolje = entity.ozn_vr_polje,
                nazPolje = entity.naz_polje,
                brojUredaja = brojUredaja
            )
        }.toMutableList()
        
        // Add virtual "Direktno na postrojenju" field if there are devices directly on facility
        val direktnoUredaji = uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
        android.util.Log.d("PostrojenjeRepository", "Checking for virtual field: found ${direktnoUredaji.size} devices directly on postrojenje $postrojenjeId")
        if (direktnoUredaji.isNotEmpty()) {
            polja.add(0, PoljeDto(
                idPolje = null, // null indicates virtual field
                napRazina = null,
                oznVrPolje = null,
                nazPolje = context.getString(com.example.elektropregled.R.string.directly_on_facility),
                brojUredaja = direktnoUredaji.size
            ))
            android.util.Log.d("PostrojenjeRepository", "Added virtual field with ${direktnoUredaji.size} devices")
        }
        
        android.util.Log.d("PostrojenjeRepository", "Built ${polja.size} polja from local DB (including virtual: ${direktnoUredaji.isNotEmpty()})")
        return Result.success(polja)
    }
    
    /**
     * Sync polja from server (background operation).
     */
    suspend fun syncPolja(postrojenjeId: Int): Result<Unit> {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            return Result.failure(Exception("Nema internetske veze"))
        }
        
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
        }
        
        return try {
            val response = apiService.getPolja(postrojenjeId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val polja = response.body()!!
                
                // Check if there's a virtual field (devices directly on facility)
                var hasVirtualField = false
                var virtualFieldDeviceCount = 0
                
                // Save to local database
                val entities = polja.mapNotNull { dto ->
                    // Check for virtual "Direktno na postrojenju" field (idPolje is null)
                    if (dto.idPolje == null) {
                        hasVirtualField = true
                        virtualFieldDeviceCount = dto.brojUredaja ?: 0
                        return@mapNotNull null // Skip saving virtual field as entity
                    }
                    
                    com.example.elektropregled.data.database.entity.PoljeEntity(
                        id_polje = dto.idPolje,
                        nap_razina = dto.napRazina ?: 0.0,
                        ozn_vr_polje = dto.oznVrPolje ?: "UNKNOWN",
                        naz_polje = dto.nazPolje,
                        id_postr = postrojenjeId
                    )
                }
                if (entities.isNotEmpty()) {
                    android.util.Log.d("PostrojenjeRepository", "Saving ${entities.size} polja to local DB")
                    poljeDao.insertAll(entities)
                } else {
                    android.util.Log.d("PostrojenjeRepository", "No polja entities to save (all were virtual)")
                }
                
                // Note: If hasVirtualField is true, devices directly on facility should be synced
                // via ChecklistRepository when user accesses the virtual field
                if (hasVirtualField && virtualFieldDeviceCount > 0) {
                    android.util.Log.d("PostrojenjeRepository", "Virtual field detected with $virtualFieldDeviceCount devices - will sync when accessed")
                }
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Greška pri učitavanju polja: ${response.code()}"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Server timeout - provjerite internet konekciju", e))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Nemogućo se povezati - server je možda odsutan", e))
        } catch (e: Exception) {
            Result.failure(Exception("Greška pri učitavanju polja: ${e.message}", e))
        }
    }
    
}
