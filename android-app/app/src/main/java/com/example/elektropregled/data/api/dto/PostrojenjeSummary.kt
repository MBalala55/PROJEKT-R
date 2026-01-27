package com.example.elektropregled.data.api.dto

data class PostrojenjeSummary(
    val idPostr: Int,
    val nazPostr: String,
    val lokacija: String?,
    val oznVrPostr: String,
    val totalPregleda: Int,
    val zadnjiPregled: String? // ISO 8601 format
)
