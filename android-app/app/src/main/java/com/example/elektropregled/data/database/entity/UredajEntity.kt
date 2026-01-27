package com.example.elektropregled.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "Uredaj",
    foreignKeys = [
        ForeignKey(
            entity = PostrojenjeEntity::class,
            parentColumns = ["id_postr"],
            childColumns = ["id_postr"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PoljeEntity::class,
            parentColumns = ["id_polje"],
            childColumns = ["id_polje"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = VrstaUredajaEntity::class,
            parentColumns = ["id_vr_ured"],
            childColumns = ["id_vr_ured"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class UredajEntity(
    @PrimaryKey
    val id_ured: Int,
    val natp_plocica: String,
    val tv_broj: String,
    val id_postr: Int,
    val id_polje: Int?,
    val id_vr_ured: Int
)
