package com.example.elektropregled.data.database.dao

import androidx.room.*
import com.example.elektropregled.data.database.entity.KorisnikEntity

@Dao
interface KorisnikDao {
    
    @Query("SELECT * FROM Korisnik WHERE id_korisnika = :id")
    suspend fun getKorisnikById(id: Int): KorisnikEntity?
    
    @Query("SELECT * FROM Korisnik WHERE korisnicko_ime = :username")
    suspend fun getKorisnikByUsername(username: String): KorisnikEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(korisnik: KorisnikEntity): Long
    
    @Update
    suspend fun update(korisnik: KorisnikEntity)
    
    @Delete
    suspend fun delete(korisnik: KorisnikEntity)
}
