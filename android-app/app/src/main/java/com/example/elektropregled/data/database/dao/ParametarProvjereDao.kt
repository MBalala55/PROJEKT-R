package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.ParametarProvjereEntity

@Dao
interface ParametarProvjereDao {
    
    @Query("SELECT * FROM ParametarProvjere WHERE id_vr_ured = :vrstaUredajaId ORDER BY redoslijed")
    suspend fun getParametriByVrstaUredaja(vrstaUredajaId: Int): List<ParametarProvjereEntity>
    
    @Query("SELECT * FROM ParametarProvjere WHERE id_parametra = :id")
    suspend fun getParametarById(id: Int): ParametarProvjereEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(parametar: ParametarProvjereEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(parametri: List<ParametarProvjereEntity>)
}
