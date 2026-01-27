package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Postrojenje")
data class PostrojenjeEntity(
    @PrimaryKey
    val id_postr: Int,
    val ozn_vr_postr: String,
    val naz_postr: String,
    val lokacija: String?
)
