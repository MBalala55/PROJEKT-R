package com.example.elektropregled.data.api.dto

data class ChecklistUredaj(
    val idUred: Int,
    val natpPlocica: String,
    val tvBroj: String,
    val oznVrUred: String,
    val nazVrUred: String,
    val idPolje: Int?,
    val nazPolje: String,
    val napRazina: Double?,
    val parametri: List<ChecklistParametar>
)
