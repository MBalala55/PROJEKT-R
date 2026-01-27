package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.UredajEntity

@Dao
interface UredajDao {
    
    @Query("SELECT * FROM Uredaj WHERE id_postr = :postrojenjeId")
    suspend fun getUredajiByPostrojenje(postrojenjeId: Int): List<UredajEntity>
    
    @Query("SELECT * FROM Uredaj WHERE id_polje = :poljeId")
    suspend fun getUredajiByPolje(poljeId: Int): List<UredajEntity>
    
    @Query("SELECT * FROM Uredaj WHERE id_postr = :postrojenjeId AND (id_polje IS NULL OR id_polje = 0)")
    suspend fun getUredajiDirectlyOnPostrojenje(postrojenjeId: Int): List<UredajEntity>
    
    @Query("SELECT * FROM Uredaj WHERE id_ured = :id")
    suspend fun getUredajById(id: Int): UredajEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(uredaj: UredajEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(uredaji: List<UredajEntity>)
}
