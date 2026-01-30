package com.example.elektropregled.data.repository

import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.*
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.PregledEntity
import com.example.elektropregled.data.database.entity.StavkaPregledaEntity
import com.example.elektropregled.data.TokenStorage
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PregledRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage
) {
    
    private val apiService = ApiClient.apiService
    private val pregledDao = database.pregledDao()
    private val stavkaDao = database.stavkaPregledaDao()
    private val checklistRepository = ChecklistRepository(database, tokenStorage)
    
    // Local operations
    suspend fun savePregledLocally(
        pregled: PregledEntity,
        stavke: List<StavkaPregledaEntity>
    ): Long {
        // Simply insert the pregled and stavke - entities already exist from loadChecklist
        val pregledId = pregledDao.insert(pregled)
        stavke.forEach { stavka ->
            stavkaDao.insert(stavka.copy(id_preg = pregledId.toInt()))
        }
        return pregledId
    }
    
    private suspend fun ensureEntitiesExist(stavke: List<StavkaPregledaEntity>) {
        val uredajDao = database.uredajDao()
        val parametarDao = database.parametarProvjereDao()
        val vrstaUredajaDao = database.vrstaUredajaDao()
        val poljeDao = database.poljeDao()
        
        // Get postrojenje ID from the first stavka's pregled (if available)
        val pregledId = stavke.firstOrNull()?.id_preg
        val pregled = if (pregledId != null) pregledDao.getPregledById(pregledId) else null
        val postrojenjeId = pregled?.id_postr ?: 0
        
        stavke.forEach { stavka ->
            // Ensure Uredaj exists
            var uredaj = uredajDao.getUredajById(stavka.id_ured)
            if (uredaj == null) {
                // Ensure default vrsta exists
                var defaultVrsta = vrstaUredajaDao.getById(1)
                if (defaultVrsta == null) {
                    vrstaUredajaDao.insert(com.example.elektropregled.data.database.entity.VrstaUredajaEntity(
                        id_vr_ured = 1,
                        ozn_vr_ured = "UNKNOWN",
                        naz_vr_ured = "Nepoznata vrsta"
                    ))
                }
                
                // Create minimal uredaj for offline mode (without polje - will be set when checklist is loaded)
                uredaj = com.example.elektropregled.data.database.entity.UredajEntity(
                    id_ured = stavka.id_ured,
                    natp_plocica = "Uređaj ${stavka.id_ured}",
                    tv_broj = "N/A",
                    id_postr = postrojenjeId,
                    id_polje = null, // Will be set when checklist is loaded
                    id_vr_ured = 1 // Default vrsta
                )
                uredajDao.insert(uredaj)
            }
            
            // If uredaj has a polje, ensure polje exists
            if (uredaj.id_polje != null && uredaj.id_polje != 0) {
                var polje = poljeDao.getPoljeById(uredaj.id_polje!!)
                if (polje == null) {
                    // Create minimal polje for offline mode
                    polje = com.example.elektropregled.data.database.entity.PoljeEntity(
                        id_polje = uredaj.id_polje!!,
                        nap_razina = 0.0,
                        ozn_vr_polje = "UNKNOWN",
                        naz_polje = "Polje ${uredaj.id_polje}",
                        id_postr = postrojenjeId
                    )
                    poljeDao.insert(polje)
                }
            }
            
            // Ensure Parametar exists
            var parametar = parametarDao.getParametarById(stavka.id_parametra)
            if (parametar == null) {
                // Create minimal parametar for offline mode
                parametar = com.example.elektropregled.data.database.entity.ParametarProvjereEntity(
                    id_parametra = stavka.id_parametra,
                    naz_parametra = "Parametar ${stavka.id_parametra}",
                    tip_podataka = when {
                        stavka.vrijednost_bool != null -> "BOOLEAN"
                        stavka.vrijednost_num != null -> "NUMERIC"
                        stavka.vrijednost_txt != null -> "TEXT"
                        else -> "BOOLEAN"
                    },
                    min_vrijednost = null,
                    max_vrijednost = null,
                    mjerna_jedinica = null,
                    obavezan = false,
                    redoslijed = 1,
                    opis = null,
                    id_vr_ured = uredaj.id_vr_ured
                )
                parametarDao.insert(parametar)
            }
            
            // Ensure VrstaUredaja exists
            val vrsta = vrstaUredajaDao.getById(uredaj.id_vr_ured)
            if (vrsta == null) {
                vrstaUredajaDao.insert(com.example.elektropregled.data.database.entity.VrstaUredajaEntity(
                    id_vr_ured = uredaj.id_vr_ured,
                    ozn_vr_ured = "UNKNOWN",
                    naz_vr_ured = "Nepoznata vrsta"
                ))
            }
        }
    }
    
    fun getAllPregledi(): Flow<List<PregledEntity>> {
        return pregledDao.getAllPregledi()
    }
    
    fun getPreglediByPostrojenje(postrojenjeId: Int): Flow<List<PregledEntity>> {
        return pregledDao.getPreglediByPostrojenje(postrojenjeId)
    }
    
    suspend fun getStavkeByPregled(pregledId: Int): List<StavkaPregledaEntity> {
        return stavkaDao.getStavkeByPregledSuspend(pregledId)
    }
    
    // Sync operations
    suspend fun syncPregledi(): SyncResult {
        android.util.Log.d("PregledRepository", "syncPregledi pozvan")
        
        val token = tokenStorage.getToken()
        android.util.Log.d("PregledRepository", "Token: ${if (token != null) "postoji" else "null"}, valjan: ${tokenStorage.isTokenValid()}")
        
        if (token == null || !tokenStorage.isTokenValid()) {
            android.util.Log.d("PregledRepository", "Token nije valjan, vraćam grešku")
            return SyncResult.Error("Token nije valjan. Molimo se ponovno prijavite.")
        }
        
        // Get pending pregledi first
        val pendingPregledi = pregledDao.getPendingPregledi()
        android.util.Log.d("PregledRepository", "Pending pregledi: ${pendingPregledi.size}")
        
        // Clean up empty pregledi, but only those that are truly empty (no stavki and not recently finished)
        // Don't delete pregledi that were just finished - they might have stavki that haven't been committed yet
        android.util.Log.d("PregledRepository", "Čistim prazne preglede...")
        cleanupEmptyPregledi()
        
        // Re-fetch pending pregledi after cleanup
        val finalPendingPregledi = pregledDao.getPendingPregledi()
        android.util.Log.d("PregledRepository", "Pending pregledi nakon cleanup: ${finalPendingPregledi.size}")
        
        if (finalPendingPregledi.isEmpty()) {
            android.util.Log.d("PregledRepository", "Nema pending pregleda za sync")
            return SyncResult.Success(0, 0, 0)
        }
        
        var syncedCount = 0
        var failedCount = 0
        var errorMessage: String? = null
        
        for (pregled in finalPendingPregledi) {
            try {
                // Update status to SYNCING
                pregledDao.updateStatus(pregled.id_preg.toInt(), "SYNCING")
                
                val stavke = stavkaDao.getStavkeByPregledSuspend(pregled.id_preg.toInt())
                
                // Skip pregledi with no stavke - they are incomplete/empty
                if (stavke.isEmpty()) {
                    pregledDao.updateStatus(pregled.id_preg.toInt(), "PENDING")
                    continue
                }
                
                // Validate that all parameter IDs exist in local database and were loaded from server
                val parametarDao = database.parametarProvjereDao()
                val invalidParams = stavke.mapNotNull { stavka ->
                    val parametar = parametarDao.getParametarById(stavka.id_parametra)
                    if (parametar == null) {
                        android.util.Log.w("PregledRepository", "Parametar ${stavka.id_parametra} not found in local database for stavka ${stavka.lokalni_id}")
                        stavka.id_parametra to "not found"
                    } else {
                        // Check if parameter was created locally (has generic name like "Parametar {id}")
                        // vs loaded from server (has actual name)
                        if (parametar.naz_parametra.startsWith("Parametar ") && 
                            parametar.naz_parametra.substringAfter("Parametar ").toIntOrNull() == stavka.id_parametra) {
                            android.util.Log.w("PregledRepository", "Parametar ${stavka.id_parametra} was created locally, not from server")
                            stavka.id_parametra to "locally created"
                        } else null
                    }
                }
                
                if (invalidParams.isNotEmpty()) {
                    val invalidIds = invalidParams.map { it.first }.distinct()
                    val errorMsg = "Neki parametri nisu validni za sinkronizaciju (ID: ${invalidIds.joinToString()}). " +
                            "Molimo učitajte checklistu s servera prije sinkronizacije."
                    android.util.Log.e("PregledRepository", errorMsg)
                    pregledDao.update(
                        pregled.copy(
                            status_sinkronizacije = "FAILED",
                            sync_error = errorMsg
                        )
                    )
                    failedCount++
                    errorMessage = errorMsg
                    continue
                }
                
                android.util.Log.d("PregledRepository", "Syncing pregled ${pregled.lokalni_id} with ${stavke.size} stavki")
                
                val syncRequest = SyncRequest(
                    pregled = PregledRequest(
                        lokalni_id = pregled.lokalni_id,
                        pocetak = pregled.pocetak,
                        kraj = pregled.kraj,
                        id_korisnika = pregled.id_korisnika,
                        id_postr = pregled.id_postr,
                        napomena = pregled.napomena
                    ),
                    stavke = stavke.map { stavka ->
                        StavkaRequest(
                            lokalni_id = stavka.lokalni_id,
                            id_ured = stavka.id_ured,
                            id_parametra = stavka.id_parametra,
                            vrijednost_bool = stavka.getBooleanValue(),
                            vrijednost_num = stavka.vrijednost_num,
                            vrijednost_txt = stavka.vrijednost_txt,
                            napomena = stavka.napomena,
                            vrijeme_unosa = stavka.vrijeme_unosa
                        )
                    }
                )
                
                val response = apiService.syncPregled(syncRequest, "Bearer $token")
                
                if (response.isSuccessful && response.body()?.success == true) {
                    val syncResponse = response.body()!!
                    
                    // Update pregled with server ID
                    pregledDao.updateServerId(pregled.lokalni_id, syncResponse.serverPregledId.toInt())
                    
                    // Update stavke with server IDs
                    syncResponse.idMappings?.stavke?.forEach { mapping ->
                        stavkaDao.updateServerId(mapping.lokalniId, mapping.serverId.toInt())
                    }
                    
                    // Update last checked time for the facility after successful sync
                    // Use the kraj time from the pregled (which should be set when finished)
                    if (pregled.kraj != null) {
                        val postrojenjeDao = database.postrojenjeDao()
                        postrojenjeDao.updateZadnjiPregled(pregled.id_postr, pregled.kraj)
                        android.util.Log.d("PregledRepository", "Updated zadnji_pregled for postrojenje ${pregled.id_postr} to ${pregled.kraj} after sync")
                    }
                    
                    syncedCount++
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Nepoznata greška"
                    android.util.Log.e("PregledRepository", "Sync failed for pregled ${pregled.lokalni_id}: $errorBody")
                    
                    // If error mentions parameter not found, try to auto-fix by reloading checklist
                    if (errorBody.contains("Parametar", ignoreCase = true)) {
                        val paramIds = stavke.map { it.id_parametra }.distinct()
                        val paramDetails = paramIds.map { paramId ->
                            val param = parametarDao.getParametarById(paramId)
                            if (param != null) {
                                "$paramId (${param.naz_parametra})"
                            } else {
                                "$paramId (not in local DB)"
                            }
                        }
                        android.util.Log.e("PregledRepository", "Parameter IDs sent: ${paramDetails.joinToString(", ")}")
                        android.util.Log.d("PregledRepository", "Attempting to auto-fix by reloading checklist from server...")
                        
                        // Try to auto-fix by fetching checklist directly from server API and remapping parameter IDs
                        try {
                            // Get all unique device IDs from stavke to determine which polja to reload
                            val deviceIds = stavke.map { it.id_ured }.distinct()
                            val uredajDao = database.uredajDao()
                            val poljeIds = deviceIds.mapNotNull { deviceId ->
                                uredajDao.getUredajById(deviceId)?.id_polje
                            }.distinct()
                            
                            // Fetch checklist directly from server API (bypass local cache) to get correct parameter IDs
                            val serverChecklistData = mutableListOf<com.example.elektropregled.data.api.dto.ChecklistUredaj>()
                            val token = tokenStorage.getToken()
                            
                            if (token != null) {
                                // Fetch from server API for each polje
                                for (poljeId in poljeIds) {
                                    try {
                                        val response = apiService.getChecklist(pregled.id_postr, poljeId, "Bearer $token")
                                        if (response.isSuccessful && response.body() != null) {
                                            val checklist = response.body()!!
                                            serverChecklistData.addAll(checklist)
                                            android.util.Log.d("PregledRepository", "Fetched ${checklist.size} devices from server API for polje $poljeId")
                                            // Save to database to update local cache
                                            checklistRepository.saveChecklistToDatabase(checklist, pregled.id_postr)
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("PregledRepository", "Error fetching checklist from API for polje $poljeId: ${e.message}")
                                    }
                                }
                                
                                // Also fetch for devices directly on facility (poljeId = 0)
                                try {
                                    val response = apiService.getChecklist(pregled.id_postr, 0, "Bearer $token")
                                    if (response.isSuccessful && response.body() != null) {
                                        val checklist = response.body()!!
                                        serverChecklistData.addAll(checklist)
                                        android.util.Log.d("PregledRepository", "Fetched ${checklist.size} devices from server API for facility direct")
                                        // Save to database to update local cache
                                        checklistRepository.saveChecklistToDatabase(checklist, pregled.id_postr)
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PregledRepository", "Error fetching checklist from API for facility direct: ${e.message}")
                                }
                            }
                            
                            if (serverChecklistData.isNotEmpty()) {
                                // Create a map: (deviceId, paramName) -> serverParamId
                                val serverParamMap = mutableMapOf<Pair<Int, String>, Int>()
                                serverChecklistData.forEach { device ->
                                    device.parametri.forEach { param ->
                                        val key = Pair(device.idUred, param.nazParametra)
                                        serverParamMap[key] = param.idParametra
                                        android.util.Log.d("PregledRepository", "Server param: device=${device.idUred}, name=${param.nazParametra}, serverId=${param.idParametra}")
                                    }
                                }
                                
                                // Update stavke with server parameter IDs, and delete stavke with parameters that don't exist on server
                                var updatedCount = 0
                                var deletedCount = 0
                                val validStavke = mutableListOf<StavkaPregledaEntity>()
                                
                                for (stavka in stavke) {
                                    val oldParam = parametarDao.getParametarById(stavka.id_parametra)
                                    val paramName = oldParam?.naz_parametra ?: ""
                                    
                                    // Check if this parameter exists on server for this device
                                    val serverParamId = serverParamMap[Pair(stavka.id_ured, paramName)]
                                    
                                    if (serverParamId == null) {
                                        // Parameter doesn't exist on server - delete this stavka
                                        stavkaDao.delete(stavka)
                                        deletedCount++
                                        android.util.Log.w("PregledRepository", "Deleted stavka with parameter '$paramName' (ID: ${stavka.id_parametra}) on device ${stavka.id_ured} - parameter doesn't exist on server")
                                    } else {
                                        // Parameter exists on server
                                        if (serverParamId != stavka.id_parametra) {
                                            // Update with server parameter ID
                                            val updatedStavka = stavka.copy(id_parametra = serverParamId)
                                            stavkaDao.update(updatedStavka)
                                            validStavke.add(updatedStavka)
                                            updatedCount++
                                            android.util.Log.d("PregledRepository", "Updated stavka parameter ID from ${stavka.id_parametra} to $serverParamId for parameter '$paramName'")
                                        } else {
                                            // Already has correct ID
                                            validStavke.add(stavka)
                                        }
                                    }
                                }
                                
                                android.util.Log.d("PregledRepository", "Updated $updatedCount stavke with server parameter IDs, deleted $deletedCount stavke with invalid parameters")
                                
                                // Only retry sync if we have valid stavke remaining
                                if (validStavke.isNotEmpty()) {
                                    android.util.Log.d("PregledRepository", "Retrying sync with ${validStavke.size} valid stavki (deleted $deletedCount invalid stavke)...")
                                    
                                    // Retry sync immediately with updated parameter IDs
                                    val retrySyncRequest = SyncRequest(
                                        pregled = PregledRequest(
                                            lokalni_id = pregled.lokalni_id,
                                            pocetak = pregled.pocetak,
                                            kraj = pregled.kraj,
                                            id_korisnika = pregled.id_korisnika,
                                            id_postr = pregled.id_postr,
                                            napomena = pregled.napomena
                                        ),
                                        stavke = validStavke.map { stavka ->
                                            StavkaRequest(
                                                lokalni_id = stavka.lokalni_id,
                                                id_ured = stavka.id_ured,
                                                id_parametra = stavka.id_parametra,
                                                vrijednost_bool = stavka.getBooleanValue(),
                                                vrijednost_num = stavka.vrijednost_num,
                                                vrijednost_txt = stavka.vrijednost_txt,
                                                napomena = stavka.napomena,
                                                vrijeme_unosa = stavka.vrijeme_unosa
                                            )
                                        }
                                    )
                                    
                                    val retryResponse = apiService.syncPregled(retrySyncRequest, "Bearer $token")
                                    
                                    if (retryResponse.isSuccessful && retryResponse.body()?.success == true) {
                                        val syncResponse = retryResponse.body()!!
                                        
                                        // Update pregled with server ID
                                        pregledDao.updateServerId(pregled.lokalni_id, syncResponse.serverPregledId.toInt())
                                        
                                        // Update stavke with server IDs
                                        syncResponse.idMappings?.stavke?.forEach { mapping ->
                                            stavkaDao.updateServerId(mapping.lokalniId, mapping.serverId.toInt())
                                        }
                                        
                                        // Update last checked time
                                        if (pregled.kraj != null) {
                                            val postrojenjeDao = database.postrojenjeDao()
                                            postrojenjeDao.updateZadnjiPregled(pregled.id_postr, pregled.kraj)
                                            android.util.Log.d("PregledRepository", "Updated zadnji_pregled for postrojenje ${pregled.id_postr} to ${pregled.kraj} after retry sync")
                                        }
                                        
                                        android.util.Log.d("PregledRepository", "Sync succeeded after auto-fixing parameter IDs!")
                                        syncedCount++
                                        continue // Success, move to next pregled
                                    } else {
                                        val retryErrorBody = retryResponse.errorBody()?.string() ?: "Nepoznata greška"
                                        android.util.Log.e("PregledRepository", "Retry sync still failed: $retryErrorBody")
                                        
                                        // Try to create minimal stavka to sync date to server
                                        if (pregled.kraj != null && serverChecklistData.isNotEmpty()) {
                                            val firstDevice = serverChecklistData.firstOrNull()
                                            val firstParam = firstDevice?.parametri?.firstOrNull()
                                            
                                            if (firstDevice != null && firstParam != null) {
                                                android.util.Log.d("PregledRepository", "Retrying with minimal stavka to sync date to server")
                                                
                                                val minimalVrijednostBool = if (firstParam.tipPodataka == "BOOLEAN") true else null
                                                val minimalVrijednostNum = if (firstParam.tipPodataka == "NUMERIC") 0.0 else null
                                                val minimalVrijednostTxt = if (firstParam.tipPodataka == "TEXT") "N/A" else null
                                                
                                                val minimalStavka = StavkaRequest(
                                                    lokalni_id = java.util.UUID.randomUUID().toString(),
                                                    id_ured = firstDevice.idUred,
                                                    id_parametra = firstParam.idParametra,
                                                    vrijednost_bool = minimalVrijednostBool,
                                                    vrijednost_num = minimalVrijednostNum,
                                                    vrijednost_txt = minimalVrijednostTxt,
                                                    napomena = "Minimalna stavka za sinkronizaciju datuma",
                                                    vrijeme_unosa = pregled.kraj
                                                )
                                                
                                                val minimalSyncRequest = SyncRequest(
                                                    pregled = PregledRequest(
                                                        lokalni_id = pregled.lokalni_id,
                                                        pocetak = pregled.pocetak,
                                                        kraj = pregled.kraj,
                                                        id_korisnika = pregled.id_korisnika,
                                                        id_postr = pregled.id_postr,
                                                        napomena = pregled.napomena
                                                    ),
                                                    stavke = listOf(minimalStavka)
                                                )
                                                
                                                val minimalSyncResponse = apiService.syncPregled(minimalSyncRequest, "Bearer $token")
                                                
                                                if (minimalSyncResponse.isSuccessful && minimalSyncResponse.body()?.success == true) {
                                                    val syncResponse = minimalSyncResponse.body()!!
                                                    pregledDao.updateServerId(pregled.lokalni_id, syncResponse.serverPregledId.toInt())
                                                    
                                                    val postrojenjeDao = database.postrojenjeDao()
                                                    postrojenjeDao.updateZadnjiPregled(pregled.id_postr, pregled.kraj)
                                                    android.util.Log.d("PregledRepository", "Updated zadnji_pregled for postrojenje ${pregled.id_postr} to ${pregled.kraj} after minimal sync (date synced to server)")
                                                    
                                                    pregledDao.update(
                                                        pregled.copy(
                                                            status_sinkronizacije = "SYNCED",
                                                            sync_error = null
                                                        )
                                                    )
                                                    syncedCount++
                                                    android.util.Log.d("PregledRepository", "Successfully synced date to server using minimal stavka after retry failed")
                                                    continue
                                                }
                                            }
                                        }
                                        // Fall through to mark as failed
                                    }
                                } else {
                                    // No valid stavke remaining, but we need to sync the date to the server
                                    // Server requires at least one stavka, so we'll create a minimal valid stavka
                                    // just to allow the sync to go through so the server updates the last checked date
                                    android.util.Log.w("PregledRepository", "No valid stavke remaining, but creating minimal stavka to sync date to server")
                                    
                                    if (pregled.kraj != null && serverChecklistData.isNotEmpty()) {
                                        // Find any valid device and parameter from the server checklist to create a minimal stavka
                                        val firstDevice = serverChecklistData.firstOrNull()
                                        val firstParam = firstDevice?.parametri?.firstOrNull()
                                        
                                        if (firstDevice != null && firstParam != null) {
                                            android.util.Log.d("PregledRepository", "Creating minimal stavka with device ${firstDevice.idUred}, parameter ${firstParam.idParametra} (${firstParam.nazParametra}) to allow date sync")
                                            
                                            // Create a minimal valid stavka based on parameter type
                                            val minimalVrijednostBool = if (firstParam.tipPodataka == "BOOLEAN") true else null
                                            val minimalVrijednostNum = if (firstParam.tipPodataka == "NUMERIC") 0.0 else null
                                            val minimalVrijednostTxt = if (firstParam.tipPodataka == "TEXT") "N/A" else null
                                            
                                            // Create a temporary stavka entity for sync (don't save to local DB)
                                            val minimalStavka = StavkaRequest(
                                                lokalni_id = java.util.UUID.randomUUID().toString(),
                                                id_ured = firstDevice.idUred,
                                                id_parametra = firstParam.idParametra,
                                                vrijednost_bool = minimalVrijednostBool,
                                                vrijednost_num = minimalVrijednostNum,
                                                vrijednost_txt = minimalVrijednostTxt,
                                                napomena = "Minimalna stavka za sinkronizaciju datuma",
                                                vrijeme_unosa = pregled.kraj
                                            )
                                            
                                            // Sync with minimal stavka to update date on server
                                            val minimalSyncRequest = SyncRequest(
                                                pregled = PregledRequest(
                                                    lokalni_id = pregled.lokalni_id,
                                                    pocetak = pregled.pocetak,
                                                    kraj = pregled.kraj,
                                                    id_korisnika = pregled.id_korisnika,
                                                    id_postr = pregled.id_postr,
                                                    napomena = pregled.napomena
                                                ),
                                                stavke = listOf(minimalStavka)
                                            )
                                            
                                            val minimalSyncResponse = apiService.syncPregled(minimalSyncRequest, "Bearer $token")
                                            
                                            if (minimalSyncResponse.isSuccessful && minimalSyncResponse.body()?.success == true) {
                                                val syncResponse = minimalSyncResponse.body()!!
                                                
                                                // Update pregled with server ID
                                                pregledDao.updateServerId(pregled.lokalni_id, syncResponse.serverPregledId.toInt())
                                                
                                                // Update last checked time (server should have updated it, but update locally too)
                                                val postrojenjeDao = database.postrojenjeDao()
                                                postrojenjeDao.updateZadnjiPregled(pregled.id_postr, pregled.kraj)
                                                android.util.Log.d("PregledRepository", "Updated zadnji_pregled for postrojenje ${pregled.id_postr} to ${pregled.kraj} after minimal sync (date synced to server)")
                                                
                                                // Mark as synced
                                                pregledDao.update(
                                                    pregled.copy(
                                                        status_sinkronizacije = "SYNCED",
                                                        sync_error = null
                                                    )
                                                )
                                                syncedCount++
                                                android.util.Log.d("PregledRepository", "Successfully synced date to server using minimal stavka")
                                                continue // Success, move to next pregled
                                            } else {
                                                val errorBody = minimalSyncResponse.errorBody()?.string() ?: "Nepoznata greška"
                                                android.util.Log.e("PregledRepository", "Minimal sync failed: $errorBody")
                                                // Fall through to mark as failed
                                            }
                                        } else {
                                            android.util.Log.e("PregledRepository", "Could not find any valid device/parameter to create minimal stavka")
                                        }
                                    }
                                    
                                    // If we couldn't create minimal stavka or sync failed, mark as failed
                                    val detailedError = "Svi parametri provjere nisu pronađeni na serveru. " +
                                            "Nije moguće sinkronizirati datum na server."
                                    pregledDao.update(
                                        pregled.copy(
                                            status_sinkronizacije = "FAILED",
                                            sync_error = detailedError
                                        )
                                    )
                                    errorMessage = detailedError
                                    failedCount++
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("PregledRepository", "Error during auto-fix attempt: ${e.message}", e)
                        }
                        
                        // If auto-fix failed, mark as failed with helpful error
                        val detailedError = "Parametri provjere nisu pronađeni na serveru. " +
                                "Pokušaj automatskog popravka nije uspio. " +
                                "Problematični ID-ovi: ${paramIds.joinToString(", ")}"
                        pregledDao.update(
                            pregled.copy(
                                status_sinkronizacije = "FAILED",
                                sync_error = detailedError
                            )
                        )
                        errorMessage = detailedError
                    } else {
                        pregledDao.update(
                            pregled.copy(
                                status_sinkronizacije = "FAILED",
                                sync_error = errorBody
                            )
                        )
                        errorMessage = errorBody
                    }
                    failedCount++
                }
            } catch (e: java.net.UnknownHostException) {
                // Network offline - keep as PENDING for auto-sync later
                pregledDao.update(
                    pregled.copy(
                        status_sinkronizacije = "PENDING",
                        sync_error = "Nema internetske veze - sinkronizacija će se izvršiti automatski kada se veza uspostavi"
                    )
                )
                failedCount++
                // Use exception message for better offline detection
                errorMessage = e.message ?: "Nema internetske veze"
            } catch (e: java.net.SocketTimeoutException) {
                pregledDao.update(
                    pregled.copy(
                        status_sinkronizacije = "PENDING",
                        sync_error = "Server timeout - pokušaj kasnije"
                    )
                )
                failedCount++
                errorMessage = "Server ne odgovara (timeout)"
            } catch (e: java.net.ConnectException) {
                pregledDao.update(
                    pregled.copy(
                        status_sinkronizacije = "PENDING",
                        sync_error = "Nemoguća konekcija - pokušaj kasnije"
                    )
                )
                failedCount++
                errorMessage = "Nemoguća konekcija na server"
            } catch (e: java.io.IOException) {
                // General network/IO error - likely offline
                pregledDao.update(
                    pregled.copy(
                        status_sinkronizacije = "PENDING",
                        sync_error = "Greška mreže - sinkronizacija će se izvršiti automatski kada se veza uspostavi"
                    )
                )
                failedCount++
                // Use exception message for better offline detection
                errorMessage = e.message ?: "Greška mreže"
            } catch (e: Exception) {
                // Other errors (e.g., server errors, validation errors) - mark as FAILED
                pregledDao.update(
                    pregled.copy(
                        status_sinkronizacije = "FAILED",
                        sync_error = e.message ?: "Nepoznata greška"
                    )
                )
                failedCount++
                errorMessage = e.message
            }
        }
        
        return if (syncedCount > 0) {
            SyncResult.Success(syncedCount, failedCount, finalPendingPregledi.size)
        } else {
            SyncResult.Error(errorMessage ?: "Sinkronizacija neuspješna")
        }
    }
    
    suspend fun createPregled(
        postrojenjeId: Int,
        napomena: String? = null
    ): PregledEntity {
        val korisnikDao = database.korisnikDao()
        
        // Get or create default user
        var userId = tokenStorage.getUserId()
        if (userId == null) {
            // Check if default user exists
            val defaultUser = korisnikDao.getKorisnikById(1)
            if (defaultUser == null) {
                // Create default user for offline mode
                val newUser = com.example.elektropregled.data.database.entity.KorisnikEntity(
                    id_korisnika = 1,
                    ime = "Default",
                    prezime = "User",
                    korisnicko_ime = "default",
                    lozinka = "",
                    uloga = "RADNIK"
                )
                korisnikDao.insert(newUser)
            }
            userId = 1
        } else {
            // Verify user exists
            val user = korisnikDao.getKorisnikById(userId)
            if (user == null) {
                // User doesn't exist, create default
                val defaultUser = korisnikDao.getKorisnikById(1)
                if (defaultUser == null) {
                    val newUser = com.example.elektropregled.data.database.entity.KorisnikEntity(
                        id_korisnika = 1,
                        ime = "Default",
                        prezime = "User",
                        korisnicko_ime = "default",
                        lozinka = "",
                        uloga = "RADNIK"
                    )
                    korisnikDao.insert(newUser)
                }
                userId = 1
            }
        }
        
        // Verify postrojenje exists, if not create a minimal one for offline mode
        var postrojenje = database.postrojenjeDao().getPostrojenjeById(postrojenjeId)
        if (postrojenje == null) {
            // Create a minimal postrojenje for offline mode
            postrojenje = com.example.elektropregled.data.database.entity.PostrojenjeEntity(
                id_postr = postrojenjeId,
                ozn_vr_postr = "UNKNOWN",
                naz_postr = "Postrojenje $postrojenjeId",
                lokacija = null
            )
            database.postrojenjeDao().insert(postrojenje)
        }
        
        val lokalniId = UUID.randomUUID().toString()
        val now = java.time.LocalDateTime.now().toString()
        
        val pregled = PregledEntity(
            lokalni_id = lokalniId,
            status_sinkronizacije = "PENDING",
            pocetak = now,
            kraj = null,
            napomena = napomena,
            id_korisnika = userId,
            id_postr = postrojenjeId
        )
        
        val id = pregledDao.insert(pregled)
        return pregled.copy(id_preg = id.toInt())
    }
    
    suspend fun finishPregled(pregledId: Int) {
        val pregled = pregledDao.getPregledById(pregledId)
        if (pregled != null) {
            val now = java.time.LocalDateTime.now().toString()
            pregledDao.update(pregled.copy(kraj = now))
            
            // Update last checked time for the facility locally (for offline mode)
            // This will be overwritten by server sync when online, but provides immediate feedback
            val postrojenjeDao = database.postrojenjeDao()
            postrojenjeDao.updateZadnjiPregled(pregled.id_postr, now)
            android.util.Log.d("PregledRepository", "Updated zadnji_pregled for postrojenje ${pregled.id_postr} to $now")
        }
    }
    
    suspend fun cleanupEmptyPregledi() {
        android.util.Log.d("PregledRepository", "cleanupEmptyPregledi pozvan")
        val pendingPregledi = pregledDao.getPendingPregledi()
        android.util.Log.d("PregledRepository", "Pending pregledi za cleanup: ${pendingPregledi.size}")
        
        val now = System.currentTimeMillis()
        
        for (pregled in pendingPregledi) {
            val stavke = stavkaDao.getStavkeByPregledSuspend(pregled.id_preg.toInt())
            android.util.Log.d("PregledRepository", "Pregled ${pregled.id_preg} ima ${stavke.size} stavki, kraj=${pregled.kraj}")
            
            if (stavke.isEmpty()) {
                // Only delete if:
                // 1. Pregled has no stavke AND
                // 2. Either kraj is not set (never finished) OR kraj was set more than 5 seconds ago (not just finished)
                val shouldDelete = if (pregled.kraj == null) {
                    // Never finished - safe to delete if empty
                    true
                } else {
                    // Recently finished - check if it's been more than 5 seconds
                    // This gives time for stavke to be committed to database
                    try {
                        val krajTime = java.time.LocalDateTime.parse(pregled.kraj)
                        val krajMillis = krajTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        val timeSinceFinish = now - krajMillis
                        // Only delete if finished more than 5 seconds ago
                        timeSinceFinish > 5000
                    } catch (e: Exception) {
                        // If we can't parse the time, be conservative and don't delete
                        android.util.Log.w("PregledRepository", "Could not parse kraj time for pregled ${pregled.id_preg}, not deleting")
                        false
                    }
                }
                
                if (shouldDelete) {
                    android.util.Log.d("PregledRepository", "Brišem prazan pregled ${pregled.id_preg}")
                    pregledDao.delete(pregled)
                } else {
                    android.util.Log.d("PregledRepository", "Ne brišem pregled ${pregled.id_preg} - možda ima stavke koje se još spremaju")
                }
            }
        }
    }
}

sealed class SyncResult {
    data class Success(val synced: Int, val failed: Int, val total: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
