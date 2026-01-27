package com.example.elektropregled.data.api.dto

data class ChecklistParametar(
    val idParametra: Int,
    val nazParametra: String,
    val tipPodataka: String, // BOOLEAN, NUMERIC, TEXT
    val minVrijednost: Double?,
    val maxVrijednost: Double?,
    val mjernaJedinica: String?,
    val obavezan: Boolean,
    val redoslijed: Int,
    val defaultVrijednostBool: Boolean?,
    val defaultVrijednostNum: Double?,
    val defaultVrijednostTxt: String?,
    val zadnjaProveraDatum: String?,
    val opis: String?
)
