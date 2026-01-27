package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "ParametarProvjere",
    foreignKeys = [
        ForeignKey(
            entity = VrstaUredajaEntity::class,
            parentColumns = ["id_vr_ured"],
            childColumns = ["id_vr_ured"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ParametarProvjereEntity(
    @PrimaryKey(autoGenerate = true)
    val id_parametra: Int = 0,
    val naz_parametra: String,
    val tip_podataka: String, // 'BOOLEAN', 'NUMERIC', 'TEXT'
    val min_vrijednost: Double?,
    val max_vrijednost: Double?,
    val mjerna_jedinica: String?,
    val obavezan: Boolean,
    val redoslijed: Int,
    val opis: String?,
    val id_vr_ured: Int
)
