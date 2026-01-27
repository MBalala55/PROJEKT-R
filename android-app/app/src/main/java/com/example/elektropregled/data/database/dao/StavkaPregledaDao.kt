package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.StavkaPregledaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StavkaPregledaDao {
    
    @Query("SELECT * FROM StavkaPregleda WHERE id_preg = :pregledId")
    fun getStavkeByPregled(pregledId: Int): Flow<List<StavkaPregledaEntity>>
    
    @Query("SELECT * FROM StavkaPregleda WHERE id_preg = :pregledId")
    suspend fun getStavkeByPregledSuspend(pregledId: Int): List<StavkaPregledaEntity>
    
    @Query("SELECT * FROM StavkaPregleda WHERE lokalni_id = :lokalniId")
    suspend fun getStavkaByLokalniId(lokalniId: String): StavkaPregledaEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stavka: StavkaPregledaEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(stavke: List<StavkaPregledaEntity>)
    
    @Update
    suspend fun update(stavka: StavkaPregledaEntity)
    
    @Delete
    suspend fun delete(stavka: StavkaPregledaEntity)
    
    @Query("UPDATE StavkaPregleda SET server_id = :serverId WHERE lokalni_id = :lokalniId")
    suspend fun updateServerId(lokalniId: String, serverId: Int)
    
    @Query("DELETE FROM StavkaPregleda WHERE id_preg = :pregledId")
    suspend fun deleteByPregled(pregledId: Int)
}
