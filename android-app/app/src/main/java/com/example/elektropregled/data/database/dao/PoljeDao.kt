package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.PoljeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoljeDao {
    
    @Query("SELECT * FROM Polje WHERE id_postr = :postrojenjeId ORDER BY naz_polje")
    fun getPoljaByPostrojenje(postrojenjeId: Int): Flow<List<PoljeEntity>>
    
    @Query("SELECT * FROM Polje WHERE id_polje = :id")
    suspend fun getPoljeById(id: Int): PoljeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(polje: PoljeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(polja: List<PoljeEntity>)
    
    @Update
    suspend fun update(polje: PoljeEntity)
    
    @Delete
    suspend fun delete(polje: PoljeEntity)
}
