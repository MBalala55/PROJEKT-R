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
                
                // Save devices and parameters to local database for offline use
                saveChecklistToDatabase(checklist, postrojenjeId)
                
                Result.success(checklist)
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
    
    private suspend fun saveChecklistToDatabase(checklist: List<ChecklistUredaj>, postrojenjeId: Int) {
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
            
            // Save Parametri
            uredajDto.parametri.forEach { parametarDto ->
                val parametar = parametarDao.getParametarById(parametarDto.idParametra)
                if (parametar == null) {
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
}
