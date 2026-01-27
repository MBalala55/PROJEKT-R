package com.example.elektropregled.data.api

import com.example.elektropregled.data.api.dto.*
import retrofit2.Response
import retrofit2.http.*

interface ElektropregledApiService {
    
    // Note: Endpoints without leading / so they combine with base URL path
    @POST("v1/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    
    @GET("v1/postrojenja")
    suspend fun getPostrojenja(
        @Header("Authorization") token: String
    ): Response<List<PostrojenjeSummary>>
    
    @GET("v1/postrojenja/{id}/polja")
    suspend fun getPolja(
        @Path("id") postrojenjeId: Int,
        @Header("Authorization") token: String
    ): Response<List<PoljeDto>>
    
    @GET("v1/postrojenja/{id}/checklist")
    suspend fun getChecklist(
        @Path("id") postrojenjeId: Int,
        @Query("id_polje") idPolje: Int,
        @Header("Authorization") token: String
    ): Response<List<ChecklistUredaj>>
    
    @POST("v1/pregled/sync")
    suspend fun syncPregled(
        @Body request: SyncRequest,
        @Header("Authorization") token: String
    ): Response<SyncResponse>
}
