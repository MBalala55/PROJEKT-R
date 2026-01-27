package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Polje",
    foreignKeys = [
        ForeignKey(
            entity = PostrojenjeEntity::class,
            parentColumns = ["id_postr"],
            childColumns = ["id_postr"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PoljeEntity(
    @PrimaryKey
    val id_polje: Int,
    val nap_razina: Double,
    val ozn_vr_polje: String,
    val naz_polje: String,
    val id_postr: Int
)
