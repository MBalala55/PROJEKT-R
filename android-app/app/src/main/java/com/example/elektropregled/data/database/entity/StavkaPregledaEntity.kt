package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "StavkaPregleda",
    foreignKeys = [
        ForeignKey(
            entity = PregledEntity::class,
            parentColumns = ["id_preg"],
            childColumns = ["id_preg"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UredajEntity::class,
            parentColumns = ["id_ured"],
            childColumns = ["id_ured"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ParametarProvjereEntity::class,
            parentColumns = ["id_parametra"],
            childColumns = ["id_parametra"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["id_preg"]),
        Index(value = ["id_ured"]),
        Index(value = ["id_preg", "id_parametra", "id_ured"], unique = true)
    ]
)
data class StavkaPregledaEntity(
    @PrimaryKey(autoGenerate = true)
    val id_stavke: Int = 0,
    val lokalni_id: String, // UUID
    val server_id: Int? = null,
    val vrijednost_bool: Int? = null, // 0 = false, 1 = true, null = not set
    val vrijednost_num: Double? = null,
    val vrijednost_txt: String? = null,
    val napomena: String? = null,
    val vrijeme_unosa: String, // ISO 8601 format
    val id_preg: Int,
    val id_ured: Int,
    val id_parametra: Int
) {
    // Helper to convert Int to Boolean
    fun getBooleanValue(): Boolean? {
        return vrijednost_bool?.let { it == 1 }
    }
    
    // Helper to set Boolean to Int
    fun setBooleanValue(value: Boolean?): StavkaPregledaEntity {
        return copy(vrijednost_bool = value?.let { if (it) 1 else 0 })
    }
}
