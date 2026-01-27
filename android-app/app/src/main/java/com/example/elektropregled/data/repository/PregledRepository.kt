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
    
    // Local operations
    suspend fun savePregledLocally(
        pregled: PregledEntity,
        stavke: List<StavkaPregledaEntity>
    ): Long {
        // Ensure all referenced entities exist before saving
        ensureEntitiesExist(stavke)
        
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
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return SyncResult.Error("Token nije valjan. Molimo se ponovno prijavite.")
        }
        
        val pendingPregledi = pregledDao.getPendingPregledi()
        if (pendingPregledi.isEmpty()) {
            return SyncResult.Success(0, 0, 0)
        }
        
        var syncedCount = 0
        var failedCount = 0
        var errorMessage: String? = null
        
        for (pregled in pendingPregledi) {
            try {
                // Update status to SYNCING
                pregledDao.updateStatus(pregled.id_preg.toInt(), "SYNCING")
                
                val stavke = stavkaDao.getStavkeByPregledSuspend(pregled.id_preg.toInt())
                
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
                    syncResponse.idMappings.stavke.forEach { mapping ->
                        stavkaDao.updateServerId(mapping.lokalniId, mapping.serverId.toInt())
                    }
                    
                    syncedCount++
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Nepoznata greška"
                    pregledDao.update(
                        pregled.copy(
                            status_sinkronizacije = "FAILED",
                            sync_error = errorBody
                        )
                    )
                    failedCount++
                    errorMessage = errorBody
                }
            } catch (e: Exception) {
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
            SyncResult.Success(syncedCount, failedCount, pendingPregledi.size)
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
        }
    }
}

sealed class SyncResult {
    data class Success(val synced: Int, val failed: Int, val total: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}
