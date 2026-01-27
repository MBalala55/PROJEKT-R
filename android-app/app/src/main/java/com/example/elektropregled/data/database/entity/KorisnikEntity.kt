package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Korisnik")
data class KorisnikEntity(
    @PrimaryKey(autoGenerate = true)
    val id_korisnika: Int = 0,
    val ime: String,
    val prezime: String,
    val korisnicko_ime: String,
    val lozinka: String,
    val uloga: String // 'ADMIN' or 'RADNIK'
)
