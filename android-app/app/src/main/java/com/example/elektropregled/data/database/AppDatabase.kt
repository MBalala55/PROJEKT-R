package com.example.elektropregled.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.elektropregled.data.database.dao.*
import com.example.elektropregled.data.database.entity.*

@Database(
    entities = [
        KorisnikEntity::class,
        PostrojenjeEntity::class,
        PoljeEntity::class,
        VrstaUredajaEntity::class,
        UredajEntity::class,
        ParametarProvjereEntity::class,
        PregledEntity::class,
        StavkaPregledaEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun korisnikDao(): KorisnikDao
    abstract fun postrojenjeDao(): PostrojenjeDao
    abstract fun poljeDao(): PoljeDao
    abstract fun vrstaUredajaDao(): VrstaUredajaDao
    abstract fun uredajDao(): UredajDao
    abstract fun parametarProvjereDao(): ParametarProvjereDao
    abstract fun pregledDao(): PregledDao
    abstract fun stavkaPregledaDao(): StavkaPregledaDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elektropregled_database"
                )
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Enable foreign keys
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enable foreign keys on every connection
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
