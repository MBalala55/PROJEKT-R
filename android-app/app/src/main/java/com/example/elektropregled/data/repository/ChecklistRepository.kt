package com.example.elektropregled.data.repository

import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.ChecklistUredaj
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.*

class ChecklistRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage
) {
    
    private val apiService = ApiClient.apiService
    
    suspend fun getChecklist(postrojenjeId: Int, poljeId: Int?): Result<List<ChecklistUredaj>> {
        // First, try to load from local database
        try {
            val uredajDao = database.uredajDao()
            val vrstaUredajaDao = database.vrstaUredajaDao()
            val parametarDao = database.parametarProvjereDao()
            val poljeDao = database.poljeDao()
            
            // Get devices based on poljeId
            val uredaji = if (poljeId == null || poljeId == 0) {
                // Devices directly on facility
                uredajDao.getUredajiDirectlyOnPostrojenje(postrojenjeId)
            } else {
                // Devices in a specific field
                uredajDao.getUredajiByPolje(poljeId)
            }
            
            if (uredaji.isNotEmpty()) {
                android.util.Log.d("ChecklistRepository", "Loading ${uredaji.size} devices from local database")
                
                val checklistUredaji = uredaji.mapNotNull { uredaj ->
                    val vrsta = vrstaUredajaDao.getById(uredaj.id_vr_ured) ?: return@mapNotNull null
                    val parametri = parametarDao.getParametriByVrstaUredaja(uredaj.id_vr_ured)
                    
                    // Get polje info if device has a polje
                    val polje = uredaj.id_polje?.let { poljeDao.getPoljeById(it) }
                    
                    // Convert parametri to ChecklistParametar
                    val checklistParametri = parametri.map { param ->
                        com.example.elektropregled.data.api.dto.ChecklistParametar(
                            idParametra = param.id_parametra,
                            nazParametra = param.naz_parametra,
                            tipPodataka = param.tip_podataka,
                            minVrijednost = param.min_vrijednost,
                            maxVrijednost = param.max_vrijednost,
                            mjernaJedinica = param.mjerna_jedinica,
                            obavezan = param.obavezan,
                            redoslijed = param.redoslijed,
                            defaultVrijednostBool = null, // Defaults handled in ViewModel
                            defaultVrijednostNum = null,
                            defaultVrijednostTxt = null,
                            zadnjaProveraDatum = null, // Can be enhanced if needed
                            opis = param.opis
                        )
                    }.sortedBy { it.redoslijed }
                    
                    com.example.elektropregled.data.api.dto.ChecklistUredaj(
                        idUred = uredaj.id_ured,
                        natpPlocica = uredaj.natp_plocica,
                        tvBroj = uredaj.tv_broj,
                        oznVrUred = vrsta.ozn_vr_ured,
                        nazVrUred = vrsta.naz_vr_ured,
                        idPolje = uredaj.id_polje,
                        nazPolje = polje?.naz_polje ?: "",
                        napRazina = polje?.nap_razina,
                        parametri = checklistParametri
                    )
                }
                
                if (checklistUredaji.isNotEmpty()) {
                    return Result.success(checklistUredaji)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ChecklistRepository", "Error loading checklist from local database", e)
        }
        
        // If local database is empty, try to fetch from API only if we have a valid token
        // But catch network errors and return appropriate offline response
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            android.util.Log.d("ChecklistRepository", "No valid token, returning empty list for offline mode")
            return Result.success(emptyList()) // Return empty list for offline mode
        }
        
        // Use 0 for devices directly on facility (when idPolje is null)
        val actualPoljeId = poljeId ?: 0
        
        return try {
            val response = apiService.getChecklist(postrojenjeId, actualPoljeId, "Bearer $token")
            if (response.isSuccessful && response.body() != null) {
                val checklist = response.body()!!
                
                // Save devices and parameters to local database for offline use
                saveChecklistToDatabase(checklist, postrojenjeId)
                
                Result.success(checklist)
            } else {
                Result.failure(Exception("Greška pri učitavanju checklistte: ${response.code()}"))
            }
        } catch (e: java.net.UnknownHostException) {
            // Offline mode - return empty list
            android.util.Log.d("ChecklistRepository", "Offline mode detected, no internet connection. Returning empty list.")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.d("ChecklistRepository", "Server timeout, returning empty list for offline mode")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: java.net.ConnectException) {
            android.util.Log.d("ChecklistRepository", "Connection exception, returning empty list for offline mode")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: java.io.IOException) {
            // General network error - offline mode
            android.util.Log.d("ChecklistRepository", "Network error (offline mode): ${e.message}")
            Result.success(emptyList()) // Return empty list for offline mode
        } catch (e: Exception) {
            android.util.Log.e("ChecklistRepository", "Unexpected error loading checklist", e)
            Result.failure(Exception("Greška pri učitavanju checklistte: ${e.message}"))
        }
    }
    
    suspend fun saveChecklistToDatabase(checklist: List<ChecklistUredaj>, postrojenjeId: Int) {
        val vrstaUredajaDao = database.vrstaUredajaDao()
        val uredajDao = database.uredajDao()
        val parametarDao = database.parametarProvjereDao()
        val poljeDao = database.poljeDao()
        
        // Map to track vrsta by ozn to avoid duplicates
        val vrstaMap = mutableMapOf<String, Int>()
        
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
            if (uredaj == null) {
                // Ensure polje exists if uredaj has a polje
                val poljeId = if (uredajDto.idPolje != null && uredajDto.idPolje != 0) {
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
                    null
                }
                
                uredajDao.insert(UredajEntity(
                    id_ured = uredajDto.idUred,
                    natp_plocica = uredajDto.natpPlocica,
                    tv_broj = uredajDto.tvBroj,
                    id_postr = postrojenjeId,
                    id_polje = poljeId,
                    id_vr_ured = vrstaId
                ))
            } else {
                // Update uredaj if polje info is missing
                if (uredaj.id_polje == null && uredajDto.idPolje != null && uredajDto.idPolje != 0) {
                    val polje = poljeDao.getPoljeById(uredajDto.idPolje)
                    if (polje == null) {
                        val newPolje = com.example.elektropregled.data.database.entity.PoljeEntity(
                            id_polje = uredajDto.idPolje,
                            nap_razina = uredajDto.napRazina ?: 0.0,
                            ozn_vr_polje = "UNKNOWN",
                            naz_polje = uredajDto.nazPolje,
                            id_postr = postrojenjeId
                        )
                        poljeDao.insert(newPolje)
                    }
                    // Note: Room doesn't support partial updates easily, so we'd need to update the whole entity
                    // For now, the polje will be created and the next time checklist is loaded, uredaj will have the correct polje
                }
            }
            
            // Save Parametri - always insert/replace to ensure we have latest server data
            // This is critical for auto-fix to work - we need to update parameters with correct server IDs
            uredajDto.parametri.forEach { parametarDto ->
                // Use insert with REPLACE to update if parameter already exists
                // This ensures we get the correct parameter IDs from the server
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
            }
        }
    }
}
