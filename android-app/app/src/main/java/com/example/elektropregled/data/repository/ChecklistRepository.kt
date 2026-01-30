package com.example.elektropregled.data.repository

import android.content.Context
import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.ChecklistParametar
import com.example.elektropregled.data.api.dto.ChecklistUredaj
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.*
import com.example.elektropregled.util.NetworkUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChecklistRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage,
    private val context: Context
) {
    
    private val apiService = ApiClient.apiService
    private val uredajDao = database.uredajDao()
    private val vrstaUredajaDao = database.vrstaUredajaDao()
    private val parametarDao = database.parametarProvjereDao()
    private val poljeDao = database.poljeDao()
    
    /**
     * Get checklist as Flow - OFFLINE-FIRST.
     * Returns data from local DB immediately, syncs with server in background when online.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getChecklistFlow(postrojenjeId: Int, poljeId: Int?): Flow<List<ChecklistUredaj>> {
        // Get Flow of devices from local DB (reactive to changes)
        val uredajiFlow = if (poljeId == null || poljeId == 0) {
            // Devices directly on facility
            uredajDao.getUredajiDirectlyOnPostrojenjeFlow(postrojenjeId)
        } else {
            // Devices in a specific field
            uredajDao.getUredajiByPoljeFlow(poljeId)
        }
        
        return uredajiFlow
            .flowOn(Dispatchers.IO)
            .catch { e ->
                android.util.Log.e("ChecklistRepository", "Error in uredajiFlow", e)
                e.printStackTrace()
                emit(emptyList<UredajEntity>())
            }
            .flatMapLatest { uredaji ->
                flow {
                    try {
                        android.util.Log.d("ChecklistRepository", "getChecklistFlow: Processing ${uredaji.size} uredaji")
                        
                        // If no devices, emit empty list immediately
                        if (uredaji.isEmpty()) {
                            android.util.Log.d("ChecklistRepository", "getChecklistFlow: No devices found, emitting empty list")
                            emit(emptyList<ChecklistUredaj>())
                            return@flow
                        }
                        
                        // Build ChecklistUredaj from local entities
                        // We need to process each device sequentially since we're calling suspend functions
                        val checklist = mutableListOf<ChecklistUredaj>()
                        
                        for (uredaj in uredaji) {
                            try {
                                val vrsta = vrstaUredajaDao.getById(uredaj.id_vr_ured) ?: run {
                                    android.util.Log.w("ChecklistRepository", "VrstaUredaja not found for id ${uredaj.id_vr_ured}")
                                    continue
                                }
                                
                                val parametri = try {
                                    parametarDao.getParametriByVrstaUredaja(uredaj.id_vr_ured)
                                } catch (e: Exception) {
                                    android.util.Log.w("ChecklistRepository", "Error getting parametri for vrsta ${uredaj.id_vr_ured}", e)
                                    emptyList()
                                }
                                
                                // Get polje info if available
                                val polje = uredaj.id_polje?.let { poljeId ->
                                    try {
                                        poljeDao.getPoljeById(poljeId)
                                    } catch (e: Exception) {
                                        android.util.Log.w("ChecklistRepository", "Error getting polje $poljeId", e)
                                        null
                                    }
                                }
                                
                                checklist.add(
                                    ChecklistUredaj(
                                        idUred = uredaj.id_ured,
                                        natpPlocica = uredaj.natp_plocica,
                                        tvBroj = uredaj.tv_broj,
                                        oznVrUred = vrsta.ozn_vr_ured,
                                        nazVrUred = vrsta.naz_vr_ured,
                                        idPolje = uredaj.id_polje,
                                        nazPolje = polje?.naz_polje ?: "",
                                        napRazina = polje?.nap_razina,
                                        parametri = parametri.map { param ->
                                            ChecklistParametar(
                                                idParametra = param.id_parametra,
                                                nazParametra = param.naz_parametra,
                                                tipPodataka = param.tip_podataka,
                                                minVrijednost = param.min_vrijednost,
                                                maxVrijednost = param.max_vrijednost,
                                                mjernaJedinica = param.mjerna_jedinica,
                                                obavezan = param.obavezan,
                                                redoslijed = param.redoslijed,
                                                defaultVrijednostBool = if (param.tip_podataka == "BOOLEAN") true else null,
                                                defaultVrijednostNum = null,
                                                defaultVrijednostTxt = null,
                                                zadnjaProveraDatum = null,
                                                opis = param.opis
                                            )
                                        }
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("ChecklistRepository", "Error processing uredaj ${uredaj.id_ured}", e)
                                e.printStackTrace()
                            }
                        }
                        
                        android.util.Log.d("ChecklistRepository", "getChecklistFlow: Emitting ${checklist.size} devices")
                        emit(checklist)
                    } catch (e: Exception) {
                        android.util.Log.e("ChecklistRepository", "Error in flow builder", e)
                        e.printStackTrace()
                        emit(emptyList<ChecklistUredaj>())
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .catch { e ->
                android.util.Log.e("ChecklistRepository", "Error in flatMapLatest", e)
                e.printStackTrace()
                emit(emptyList<ChecklistUredaj>())
            }
    }
    
    /**
     * Get checklist (backward compatibility) - OFFLINE-FIRST.
     * Always returns data from local DB, never requires network.
     */
    suspend fun getChecklist(postrojenjeId: Int, poljeId: Int?): Result<List<ChecklistUredaj>> {
        return try {
            android.util.Log.d("ChecklistRepository", "getChecklist: postrojenjeId=$postrojenjeId, poljeId=$poljeId")
            
            // Get from local DB - this is the single source of truth for offline mode
            val uredaji = if (poljeId == null || poljeId == 0) {
                uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
            } else {
                uredajDao.getUredajiByPolje(poljeId)
            }
            
            android.util.Log.d("ChecklistRepository", "Found ${uredaji.size} uredaji in local DB")
            
            // Always build from local data, even if empty
            buildChecklistFromLocal(uredaji)
        } catch (e: Exception) {
            android.util.Log.e("ChecklistRepository", "Error getting checklist from local DB", e)
            Result.failure(Exception("Greška pri učitavanju checklistte: ${e.message}", e))
        }
    }
    
    /**
     * Sync ALL checklist data for a postrojenje (all polja including virtual field).
     * This ensures all data is available offline.
     */
    suspend fun syncAllChecklistForPostrojenje(postrojenjeId: Int): Result<Unit> {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            return Result.failure(Exception("Nema internetske veze"))
        }
        
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
        }
        
        return try {
            // First, sync all polja to get the list
            val poljaResponse = apiService.getPolja(
                postrojenjeId, 
                "Bearer $token"
            )
            
            if (!poljaResponse.isSuccessful || poljaResponse.body() == null) {
                return Result.failure(Exception("Greška pri učitavanju polja: ${poljaResponse.code()}"))
            }
            
            val polja = poljaResponse.body()!!
            android.util.Log.d("ChecklistRepository", "Syncing checklist for ${polja.size} polja in postrojenje $postrojenjeId")
            
            // Sync checklist for each polje (including virtual field with poljeId = null)
            var successCount = 0
            var errorCount = 0
            
            for (polje in polja) {
                val poljeId = if (polje.idPolje == null) null else polje.idPolje
                val result = syncChecklist(postrojenjeId, poljeId)
                if (result.isSuccess) {
                    successCount++
                } else {
                    errorCount++
                    android.util.Log.w("ChecklistRepository", "Failed to sync checklist for polje $poljeId: ${result.exceptionOrNull()?.message}")
                }
            }
            
            android.util.Log.d("ChecklistRepository", "Sync completed: $successCount successful, $errorCount failed")
            
            if (errorCount == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Sync completed with $errorCount errors"))
            }
        } catch (e: Exception) {
            android.util.Log.e("ChecklistRepository", "Error syncing all checklist", e)
            Result.failure(Exception("Greška pri sinkronizaciji checklistte: ${e.message}", e))
        }
    }
    
    /**
     * Trigger background sync for checklist if online (non-blocking, fails silently if offline).
     */
    fun triggerSyncChecklistIfOnline(scope: CoroutineScope, postrojenjeId: Int, poljeId: Int?) {
        scope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isNetworkAvailable(context)) {
                    syncChecklist(postrojenjeId, poljeId)
                }
            } catch (e: Exception) {
                // Silently fail - app works offline with local data
                android.util.Log.d("ChecklistRepository", "Background sync checklist failed (offline?): ${e.message}")
            }
        }
    }
    
    /**
     * Sync checklist from server (background operation).
     */
    suspend fun syncChecklist(postrojenjeId: Int, poljeId: Int?): Result<Unit> {
        if (!NetworkUtil.isNetworkAvailable(context)) {
            return Result.failure(Exception("Nema internetske veze"))
        }
        
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
        }
        
        // Use 0 for devices directly on facility (when idPolje is null)
        val actualPoljeId = poljeId ?: 0
        
        return try {
            val response = apiService.getChecklist(postrojenjeId, actualPoljeId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val checklist = response.body()!!
                android.util.Log.d("ChecklistRepository", "Sync successful: received ${checklist.size} devices")
                
                // Save devices and parameters to local database
                saveChecklistToDatabase(checklist, postrojenjeId)
                android.util.Log.d("ChecklistRepository", "Saved checklist to local DB")
                
                Result.success(Unit)
            } else {
                Result.failure(Exception("Greška pri učitavanju checklistte: ${response.code()}"))
            }
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(Exception("Server timeout - provjerite internet konekciju", e))
        } catch (e: java.net.ConnectException) {
            Result.failure(Exception("Nemogućo se povezati - server je možda odsutan", e))
        } catch (e: Exception) {
            Result.failure(Exception("Greška pri učitavanju checklistte: ${e.message}", e))
        }
    }
    
    /**
     * Trigger background sync if online (non-blocking, fails silently if offline).
     */
    fun triggerSyncIfOnline(scope: CoroutineScope, postrojenjeId: Int, poljeId: Int?) {
        scope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtil.isNetworkAvailable(context)) {
                    syncChecklist(postrojenjeId, poljeId)
                }
            } catch (e: Exception) {
                // Silently fail - app works offline with local data
                android.util.Log.d("ChecklistRepository", "Background sync failed (offline?): ${e.message}")
            }
        }
    }
    
    private suspend fun buildChecklistFromLocal(uredaji: List<UredajEntity>): Result<List<ChecklistUredaj>> {
        val checklist = uredaji.mapNotNull { uredaj ->
            val vrsta = vrstaUredajaDao.getById(uredaj.id_vr_ured) ?: return@mapNotNull null
            val parametri = parametarDao.getParametriByVrstaUredaja(uredaj.id_vr_ured)
            
            // Get polje info if available
            val polje = uredaj.id_polje?.let { poljeDao.getPoljeById(it) }
            
            ChecklistUredaj(
                idUred = uredaj.id_ured,
                natpPlocica = uredaj.natp_plocica,
                tvBroj = uredaj.tv_broj,
                oznVrUred = vrsta.ozn_vr_ured,
                nazVrUred = vrsta.naz_vr_ured,
                idPolje = uredaj.id_polje,
                nazPolje = polje?.naz_polje ?: "",
                napRazina = polje?.nap_razina,
                parametri = parametri.map { param ->
                    ChecklistParametar(
                        idParametra = param.id_parametra,
                        nazParametra = param.naz_parametra,
                        tipPodataka = param.tip_podataka,
                        minVrijednost = param.min_vrijednost,
                        maxVrijednost = param.max_vrijednost,
                        mjernaJedinica = param.mjerna_jedinica,
                        obavezan = param.obavezan,
                        redoslijed = param.redoslijed,
                        defaultVrijednostBool = if (param.tip_podataka == "BOOLEAN") true else null,
                        defaultVrijednostNum = null,
                        defaultVrijednostTxt = null,
                        zadnjaProveraDatum = null,
                        opis = param.opis
                    )
                }
            )
        }
        
        return Result.success(checklist)
    }
    
    private suspend fun saveChecklistToDatabase(checklist: List<ChecklistUredaj>, postrojenjeId: Int) {
        android.util.Log.d("ChecklistRepository", "saveChecklistToDatabase: Saving ${checklist.size} devices for postrojenje $postrojenjeId")
        
        val vrstaUredajaDao = database.vrstaUredajaDao()
        val uredajDao = database.uredajDao()
        val parametarDao = database.parametarProvjereDao()
        val poljeDao = database.poljeDao()
        
        // Map to track vrsta by ozn to avoid duplicates
        val vrstaMap = mutableMapOf<String, Int>()
        
        var savedDevices = 0
        var savedParams = 0
        
        checklist.forEach { uredajDto ->
            // Ensure Polje exists if uredaj has a polje
            if (uredajDto.idPolje != null && uredajDto.idPolje != 0) {
                var polje = poljeDao.getPoljeById(uredajDto.idPolje)
                if (polje == null) {
                    // Create minimal polje for offline mode
                    polje = com.example.elektropregled.data.database.entity.PoljeEntity(
                        id_polje = uredajDto.idPolje,
                        nap_razina = uredajDto.napRazina ?: 0.0,
                        ozn_vr_polje = "UNKNOWN",
                        naz_polje = uredajDto.nazPolje,
                        id_postr = postrojenjeId
                    )
                    poljeDao.insert(polje)
                }
            }
            // Get or create VrstaUredaja by ozn
            var vrstaId: Int = vrstaMap[uredajDto.oznVrUred] ?: run {
                // Try to find existing by ozn
                val existing = vrstaUredajaDao.getByOzn(uredajDto.oznVrUred)
                
                if (existing != null) {
                    existing.id_vr_ured
                } else {
                    // Create new vrsta - use a simple auto-increment approach
                    // Since we don't have the actual ID from server, we'll use a hash-based approach
                    var newVrstaId = uredajDto.oznVrUred.hashCode().let { if (it < 0) -it else it }
                    if (newVrstaId == 0) newVrstaId = 1
                    
                    // Check if this ID already exists
                    var finalVrstaId = newVrstaId
                    var counter = 0
                    while (vrstaUredajaDao.getById(finalVrstaId) != null && counter < 1000) {
                        finalVrstaId = (newVrstaId + counter) % Int.MAX_VALUE
                        counter++
                    }
                    
                    vrstaUredajaDao.insert(VrstaUredajaEntity(
                        id_vr_ured = finalVrstaId,
                        ozn_vr_ured = uredajDto.oznVrUred,
                        naz_vr_ured = uredajDto.nazVrUred
                    ))
                    
                    finalVrstaId
                }
            }
            
            // Store in map for future use
            if (!vrstaMap.containsKey(uredajDto.oznVrUred)) {
                vrstaMap[uredajDto.oznVrUred] = vrstaId
            }
            
            // Save Uredaj
            val uredaj = uredajDao.getUredajById(uredajDto.idUred)
            
            // Determine polje ID - null means device is directly on facility
            val finalPoljeId = if (uredajDto.idPolje != null && uredajDto.idPolje != 0) {
                // Ensure polje exists if uredaj has a polje
                val polje = poljeDao.getPoljeById(uredajDto.idPolje)
                if (polje == null) {
                    // Create minimal polje
                    val newPolje = com.example.elektropregled.data.database.entity.PoljeEntity(
                        id_polje = uredajDto.idPolje,
                        nap_razina = uredajDto.napRazina ?: 0.0,
                        ozn_vr_polje = "UNKNOWN",
                        naz_polje = uredajDto.nazPolje,
                        id_postr = postrojenjeId
                    )
                    poljeDao.insert(newPolje)
                }
                uredajDto.idPolje
            } else {
                null // Devices directly on facility have null polje
            }
            
            if (uredaj == null) {
                uredajDao.insert(UredajEntity(
                    id_ured = uredajDto.idUred,
                    natp_plocica = uredajDto.natpPlocica,
                    tv_broj = uredajDto.tvBroj,
                    id_postr = postrojenjeId,
                    id_polje = finalPoljeId,
                    id_vr_ured = vrstaId
                ))
                savedDevices++
                android.util.Log.d("ChecklistRepository", "Saved device ${uredajDto.idUred} with poljeId=$finalPoljeId (null = directly on facility)")
            } else {
                // Update device if polje changed or is missing
                if (uredaj.id_polje != finalPoljeId) {
                    uredajDao.insert(UredajEntity(
                        id_ured = uredaj.id_ured,
                        natp_plocica = uredaj.natp_plocica,
                        tv_broj = uredaj.tv_broj,
                        id_postr = uredaj.id_postr,
                        id_polje = finalPoljeId,
                        id_vr_ured = uredaj.id_vr_ured
                    ))
                    android.util.Log.d("ChecklistRepository", "Updated device ${uredajDto.idUred} poljeId from ${uredaj.id_polje} to $finalPoljeId")
                }
            }
            
            // Save Parametri (use REPLACE strategy - DAO already configured for this)
            uredajDto.parametri.forEach { parametarDto ->
                // Check if parameter exists - if it does, it might be from seed DB with different vrsta
                val existingParam = parametarDao.getParametarById(parametarDto.idParametra)
                if (existingParam == null || existingParam.id_vr_ured != vrstaId) {
                    // Insert or replace to ensure correct vrsta linkage
                    parametarDao.insert(ParametarProvjereEntity(
                        id_parametra = parametarDto.idParametra,
                        naz_parametra = parametarDto.nazParametra,
                        tip_podataka = parametarDto.tipPodataka,
                        min_vrijednost = parametarDto.minVrijednost,
                        max_vrijednost = parametarDto.maxVrijednost,
                        mjerna_jedinica = parametarDto.mjernaJedinica,
                        obavezan = parametarDto.obavezan,
                        redoslijed = parametarDto.redoslijed,
                        opis = parametarDto.opis,
                        id_vr_ured = vrstaId
                    ))
                    savedParams++
                    android.util.Log.d("ChecklistRepository", "Saved parameter ${parametarDto.idParametra} for vrsta $vrstaId")
                } else {
                    android.util.Log.d("ChecklistRepository", "Parameter ${parametarDto.idParametra} already exists with correct vrsta")
                }
            }
        }
        
        android.util.Log.d("ChecklistRepository", "saveChecklistToDatabase completed: Saved $savedDevices devices, $savedParams parameters")
    }
}
