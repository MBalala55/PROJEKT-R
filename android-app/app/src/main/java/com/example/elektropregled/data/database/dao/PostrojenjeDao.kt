package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.PostrojenjeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostrojenjeDao {
    
    @Query("SELECT * FROM Postrojenje ORDER BY naz_postr")
    fun getAllPostrojenja(): Flow<List<PostrojenjeEntity>>
    
    @Query("SELECT * FROM Postrojenje WHERE id_postr = :id")
    suspend fun getPostrojenjeById(id: Int): PostrojenjeEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(postrojenje: PostrojenjeEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(postrojenja: List<PostrojenjeEntity>)
    
    @Update
    suspend fun update(postrojenje: PostrojenjeEntity)
    
    @Delete
    suspend fun delete(postrojenje: PostrojenjeEntity)
}
