package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "VrstaUredaja")
data class VrstaUredajaEntity(
    @PrimaryKey(autoGenerate = true)
    val id_vr_ured: Int = 0,
    val ozn_vr_ured: String,
    val naz_vr_ured: String
)
