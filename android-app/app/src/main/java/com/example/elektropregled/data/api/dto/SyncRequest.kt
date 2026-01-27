package com.example.elektropregled.data.api.dto

data class SyncRequest(
    val pregled: PregledRequest,
    val stavke: List<StavkaRequest>
)

data class PregledRequest(
    val lokalni_id: String,
    val pocetak: String, // ISO 8601
    val kraj: String?,
    val id_korisnika: Int,
    val id_postr: Int,
    val napomena: String?
)

data class StavkaRequest(
    val lokalni_id: String,
    val id_ured: Int,
    val id_parametra: Int,
    val vrijednost_bool: Boolean?,
    val vrijednost_num: Double?,
    val vrijednost_txt: String?,
    val napomena: String?,
    val vrijeme_unosa: String? // ISO 8601
)
