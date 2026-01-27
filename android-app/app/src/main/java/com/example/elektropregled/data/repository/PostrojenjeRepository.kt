package com.example.elektropregled.data.repository

import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.PostrojenjeEntity

class PostrojenjeRepository(
    private val database: AppDatabase,
    private val tokenStorage: TokenStorage
) {
    
    private val apiService = ApiClient.apiService
    private val postrojenjeDao = database.postrojenjeDao()
    
    suspend fun getPostrojenja(): Result<List<PostrojenjeSummary>> {
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
                        lokacija = summary.lokacija
                    )
                }
                postrojenjeDao.insertAll(entities)
                
                Result.success(facilities)
            } else {
                Result.failure(Exception("Greška pri učitavanju postrojenja: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPolja(postrojenjeId: Int): Result<List<PoljeDto>> {
        val token = tokenStorage.getToken()
        if (token == null || !tokenStorage.isTokenValid()) {
            return Result.failure(Exception("Token nije valjan"))
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
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
