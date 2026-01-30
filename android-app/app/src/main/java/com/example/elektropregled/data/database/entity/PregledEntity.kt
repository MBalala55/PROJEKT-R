package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Pregled",
    foreignKeys = [
        ForeignKey(
            entity = PostrojenjeEntity::class,
            parentColumns = ["id_postr"],
            childColumns = ["id_postr"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = KorisnikEntity::class,
            parentColumns = ["id_korisnika"],
            childColumns = ["id_korisnika"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["status_sinkronizacije"]),
        Index(value = ["id_korisnika"]),
        Index(value = ["id_postr"])
    ]
)
data class PregledEntity(
    @PrimaryKey(autoGenerate = true)
    val id_preg: Int = 0,
    val lokalni_id: String, // UUID
    val server_id: Int? = null,
    @androidx.room.ColumnInfo(defaultValue = "PENDING")
    val status_sinkronizacije: String = "PENDING", // PENDING, SYNCING, SYNCED, FAILED
    val pocetak: String, // ISO 8601 format
    val kraj: String? = null,
    val napomena: String? = null,
    val sync_error: String? = null,
    val id_korisnika: Int,
    val id_postr: Int
)
