package com.example.elektropregled.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ParametarProvjere",
    foreignKeys = [
        ForeignKey(
            entity = VrstaUredajaEntity::class,
            parentColumns = ["id_vr_ured"],
            childColumns = ["id_vr_ured"],
            onDelete = ForeignKey.NO_ACTION  // Match seed DB schema - no ON DELETE specified means NO ACTION
        )
    ],
    indices = [
        Index(value = ["id_vr_ured"], name = "idx_parametar_vrsta"),
        Index(value = ["id_vr_ured", "redoslijed"], name = "idx_parametar_redoslijed")
    ]
)
data class ParametarProvjereEntity(
    @PrimaryKey(autoGenerate = true)
    val id_parametra: Int,
    val naz_parametra: String,
    val tip_podataka: String, // 'BOOLEAN', 'NUMERIC', 'TEXT'
    val min_vrijednost: Double?,
    val max_vrijednost: Double?,
    val mjerna_jedinica: String?,
    @ColumnInfo(defaultValue = "1")
    val obavezan: Boolean,
    val redoslijed: Int,
    val opis: String?,
    val id_vr_ured: Int
)
