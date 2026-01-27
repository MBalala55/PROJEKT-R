package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.VrstaUredajaEntity

@Dao
interface VrstaUredajaDao {
    
    @Query("SELECT * FROM VrstaUredaja")
    suspend fun getAll(): List<VrstaUredajaEntity>
    
    @Query("SELECT * FROM VrstaUredaja WHERE id_vr_ured = :id")
    suspend fun getById(id: Int): VrstaUredajaEntity?
    
    @Query("SELECT * FROM VrstaUredaja WHERE ozn_vr_ured = :ozn")
    suspend fun getByOzn(ozn: String): VrstaUredajaEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vrsta: VrstaUredajaEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vrste: List<VrstaUredajaEntity>)
}
