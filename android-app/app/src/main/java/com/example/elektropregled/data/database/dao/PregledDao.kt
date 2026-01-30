package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.PregledEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PregledDao {
    
    @Query("SELECT * FROM Pregled ORDER BY pocetak DESC")
    fun getAllPregledi(): Flow<List<PregledEntity>>
    
    @Query("SELECT * FROM Pregled WHERE status_sinkronizacije = 'PENDING' OR status_sinkronizacije = 'FAILED'")
    suspend fun getPendingPregledi(): List<PregledEntity>
    
    @Query("SELECT * FROM Pregled WHERE id_preg = :id")
    suspend fun getPregledById(id: Int): PregledEntity?
    
    @Query("SELECT * FROM Pregled WHERE lokalni_id = :lokalniId")
    suspend fun getPregledByLokalniId(lokalniId: String): PregledEntity?
    
    @Query("SELECT * FROM Pregled WHERE id_postr = :postrojenjeId ORDER BY pocetak DESC")
    fun getPreglediByPostrojenje(postrojenjeId: Int): Flow<List<PregledEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pregled: PregledEntity): Long
    
    @Update
    suspend fun update(pregled: PregledEntity)
    
    @Delete
    suspend fun delete(pregled: PregledEntity)
    
    @Query("UPDATE Pregled SET status_sinkronizacije = :status WHERE id_preg = :id")
    suspend fun updateStatus(id: Int, status: String)
    
    @Query("UPDATE Pregled SET server_id = :serverId, status_sinkronizacije = 'SYNCED' WHERE lokalni_id = :lokalniId")
    suspend fun updateServerId(lokalniId: String, serverId: Int)
    
    @Query("SELECT COUNT(*) FROM Pregled WHERE id_postr = :postrojenjeId")
    suspend fun getPregledCountByPostrojenje(postrojenjeId: Int): Int
    
    @Query("SELECT * FROM Pregled WHERE id_postr = :postrojenjeId ORDER BY pocetak DESC LIMIT 1")
    suspend fun getLastPregledByPostrojenje(postrojenjeId: Int): PregledEntity?
}
