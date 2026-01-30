package com.example.elektropregled.data.repository

import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.PostrojenjeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PostrojenjeRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage
) {
    
    private val apiService = ApiClient.apiService
    private val postrojenjeDao = database.postrojenjeDao()
    private val pregledDao = database.pregledDao()
    
    // Background scope for sync operations
    private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    /**
     * Returns a Flow that continuously observes the database and automatically updates
     * when data changes (e.g., after sync). Also triggers background sync on first load.
     */
    fun getPostrojenjaFlow(): Flow<List<PostrojenjeSummary>> {
        return postrojenjeDao.getAllPostrojenja()
            .onStart {
                // Trigger sync on first emission (non-blocking)
                syncScope.launch {
                    try {
                        val localPostrojenja = postrojenjeDao.getAllPostrojenja().first()
                        if (localPostrojenja.isNotEmpty()) {
                            android.util.Log.d("PostrojenjeRepository", "Triggering background sync for ${localPostrojenja.size} facilities")
                            syncLastCheckFromServer()
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("PostrojenjeRepository", "Error in background sync: ${e.message}")
                    }
                }
            }
            .flatMapLatest { localPostrojenja ->
                if (localPostrojenja.isEmpty()) {
                    // If local database is empty, try to fetch from API
                    flow {
                        val result = getPostrojenja()
                        emit(result.getOrElse { emptyList() })
                    }
                } else {
                    // Convert entities to summaries - use flow builder to handle suspend functions
                    flow {
                        val summaries = localPostrojenja.map { entity ->
                            // Use server-synced last check date if available
                            var lastCheckDate = entity.zadnji_pregled
                            
                            // If no server-synced date, check local pregledi
                            if (lastCheckDate == null) {
                                try {
                                    val pregledi = pregledDao.getPreglediByPostrojenje(entity.id_postr).first()
                                    val finishedPregledi = pregledi.filter { it.kraj != null }
                                    val lastPregled = finishedPregledi.maxByOrNull { it.kraj ?: "" }
                                    lastCheckDate = lastPregled?.kraj
                                } catch (e: Exception) {
                                    // If we can't get pregledi, just use null
                                    android.util.Log.d("PostrojenjeRepository", "Error getting pregledi for ${entity.id_postr}: ${e.message}")
                                }
                            }
                            
                            PostrojenjeSummary(
                                idPostr = entity.id_postr,
                                nazPostr = entity.naz_postr,
                                lokacija = entity.lokacija,
                                oznVrPostr = entity.ozn_vr_postr,
                                totalPregleda = 0, // Not used in UI
                                zadnjiPregled = lastCheckDate,
                                zadnjiKorisnik = null // Can be enhanced if needed
                            )
                        }
                        emit(summaries)
                    }
                }
            }
            .distinctUntilChanged { old, new ->
                // Only emit if the list actually changed (by comparing IDs and last check dates)
                old.size == new.size && old.zip(new).all { (oldItem, newItem) ->
                    oldItem.idPostr == newItem.idPostr && oldItem.zadnjiPregled == newItem.zadnjiPregled
                }
            }
    }
    
    suspend fun getPostrojenja(): Result<List<PostrojenjeSummary>> {
        // First, try to load from local database
        try {
            val localPostrojenja = postrojenjeDao.getAllPostrojenja().first()
            
            if (localPostrojenja.isNotEmpty()) {
                android.util.Log.d("PostrojenjeRepository", "Loading ${localPostrojenja.size} facilities from local database")
                
                // Convert to PostrojenjeSummary with last check info
                // Prioritize server-synced zadnji_pregled, fallback to local pregledi
                val summaries = localPostrojenja.map { entity ->
                    // Use server-synced last check date if available
                    var lastCheckDate = entity.zadnji_pregled
                    
                    // If no server-synced date, check local pregledi
                    if (lastCheckDate == null) {
                        val pregledi = pregledDao.getPreglediByPostrojenje(entity.id_postr).first()
                        val finishedPregledi = pregledi.filter { it.kraj != null }
                        val lastPregled = finishedPregledi.maxByOrNull { it.kraj ?: "" }
                        lastCheckDate = lastPregled?.kraj
                    }
                    
                    PostrojenjeSummary(
                        idPostr = entity.id_postr,
                        nazPostr = entity.naz_postr,
                        lokacija = entity.lokacija,
                        oznVrPostr = entity.ozn_vr_postr,
                        totalPregleda = 0, // Not used in UI
                        zadnjiPregled = lastCheckDate,
                        zadnjiKorisnik = null // Can be enhanced if needed
                    )
                }
                
                // Try to sync last check from server in background (non-blocking)
                syncLastCheckFromServer()
                
                return Result.success(summaries)
            }
        } catch (e: Exception) {
            android.util.Log.e("PostrojenjeRepository", "Error loading from local database", e)
        }
        
        // If local database is empty, try to fetch from API
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
        }
        
        return try {
            val response = apiService.getPostrojenja("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val facilities = response.body()!!
                
                // Save to local database for offline use
                val entities = facilities.map { summary ->
                    PostrojenjeEntity(
                        id_postr = summary.idPostr,
                        ozn_vr_postr = summary.oznVrPostr,
                        naz_postr = summary.nazPostr,
                        lokacija = summary.lokacija,
                        zadnji_pregled = summary.zadnjiPregled // Save last check date from server
                    )
                }
                postrojenjeDao.insertAll(entities)
                
                Result.success(facilities)
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
    
    private suspend fun syncLastCheckFromServer() {
        // This runs in background to sync only the last check info
        // without blocking the UI
        try {
            val token = tokenStorage.getToken()
            if (token == null || !tokenStorage.isTokenValid()) {
                return
            }
            
            val response = apiService.getPostrojenja("Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val serverSummaries = response.body()!!
                
                // Update only the zadnji_pregled field for each postrojenje
                var updateCount = 0
                serverSummaries.forEach { summary ->
                    if (summary.zadnjiPregled != null) {
                        val entity = postrojenjeDao.getPostrojenjeById(summary.idPostr)
                        if (entity != null) {
                            // Only update if server has a newer date or local doesn't have one
                            val shouldUpdate = if (entity.zadnji_pregled == null) {
                                true
                            } else {
                                // Compare dates as strings (ISO 8601 format is sortable)
                                summary.zadnjiPregled > entity.zadnji_pregled
                            }
                            
                            if (shouldUpdate) {
                                // Use direct SQL update to avoid affecting other fields and ensure Flow emits
                                postrojenjeDao.updateZadnjiPregled(summary.idPostr, summary.zadnjiPregled)
                                updateCount++
                            }
                        }
                    }
                }
                
                if (updateCount > 0) {
                    android.util.Log.d("PostrojenjeRepository", "Updated last check dates for $updateCount facilities from server")
                } else {
                    android.util.Log.d("PostrojenjeRepository", "Last check dates already up to date")
                }
            }
        } catch (e: Exception) {
            // Silently fail - offline mode should still work
            android.util.Log.d("PostrojenjeRepository", "Could not sync last check from server: ${e.message}")
        }
    }
    
    suspend fun getPolja(postrojenjeId: Int): Result<List<PoljeDto>> {
        // First, try to load from local database
        try {
            val poljeDao = database.poljeDao()
            val uredajDao = database.uredajDao()
            val localPolja = poljeDao.getPoljaByPostrojenje(postrojenjeId).first()
            
            if (localPolja.isNotEmpty()) {
                android.util.Log.d("PostrojenjeRepository", "Loading ${localPolja.size} fields from local database for postrojenje $postrojenjeId")
                
                // Convert to PoljeDto
                val poljaDtos = localPolja.map { entity ->
                    val uredaji = uredajDao.getUredajiByPolje(entity.id_polje)
                    PoljeDto(
                        idPolje = entity.id_polje,
                        nazPolje = entity.naz_polje,
                        napRazina = entity.nap_razina,
                        oznVrPolje = entity.ozn_vr_polje,
                        brojUredaja = uredaji.size
                    )
                }
                
                // Add virtual "Direktno na postrojenju" field if there are devices directly on facility
                val uredajiNaPostrojenju = uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
                if (uredajiNaPostrojenju.isNotEmpty()) {
                    val virtualPolje = PoljeDto(
                        idPolje = null,
                        nazPolje = "Direktno na postrojenju",
                        napRazina = null,
                        oznVrPolje = null,
                        brojUredaja = uredajiNaPostrojenju.size
                    )
                    return Result.success(listOf(virtualPolje) + poljaDtos)
                }
                
                return Result.success(poljaDtos)
            } else {
                android.util.Log.d("PostrojenjeRepository", "No local fields found for postrojenje $postrojenjeId")
            }
        } catch (e: Exception) {
            android.util.Log.e("PostrojenjeRepository", "Error loading fields from local database", e)
        }
        
        // If local database is empty, try to fetch from API only if we have a valid token
        // But catch network errors and return appropriate offline message
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            android.util.Log.d("PostrojenjeRepository", "No valid token, returning empty list for offline mode")
            return Result.failure(Exception("Nema podataka u lokalnoj bazi. Prijavite se kada imate internet konekciju."))
        }
        
        return try {
            val response = apiService.getPolja(postrojenjeId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val polja = response.body()!!
                
                // Save to local database for offline use
                val poljeDao = database.poljeDao()
                val entities = polja.mapNotNull { dto ->
                    // Skip virtual "Direktno na postrojenju" field (idPolje is null)
                    if (dto.idPolje == null) return@mapNotNull null
                    
                    com.example.elektropregled.data.database.entity.PoljeEntity(
                        id_polje = dto.idPolje,
                        nap_razina = dto.napRazina ?: 0.0,
                        ozn_vr_polje = dto.oznVrPolje ?: "UNKNOWN",
                        naz_polje = dto.nazPolje,
                        id_postr = postrojenjeId
                    )
                }
                if (entities.isNotEmpty()) {
                    poljeDao.insertAll(entities)
                }
                
                Result.success(polja)
            } else {
                Result.failure(Exception("Greška pri učitavanju polja: ${response.code()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            // Offline mode - return empty list since local data wasn't found
            android.util.Log.d("PostrojenjeRepository", "Offline mode detected, no internet connection. Returning empty list.")
            Result.success(emptyList()) // Return empty list instead of error for offline mode
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.d("PostrojenjeRepository", "Server timeout, returning empty list for offline mode")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: java.net.ConnectException) {
            android.util.Log.d("PostrojenjeRepository", "Connection exception, returning empty list for offline mode")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: java.io.IOException) {
            // General network error - offline mode
            android.util.Log.d("PostrojenjeRepository", "Network error (offline mode): ${e.message}")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: Exception) {
            android.util.Log.e("PostrojenjeRepository", "Unexpected error loading fields", e)
            Result.failure(Exception("Greška pri učitavanju polja: ${e.message}"))
        }
    }
}
