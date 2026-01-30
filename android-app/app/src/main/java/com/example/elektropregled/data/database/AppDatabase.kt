package com.example.elektropregled.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.elektropregled.data.database.dao.*
import com.example.elektropregled.data.database.entity.*
import java.io.File

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
    version = 3,
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
        private const val DB_NAME = "elektropregled_database"
        private const val SEED_DB_ASSET_PATH = "database/seed.db"
        private const val PREF_SEED_COPIED = "seed_db_copied"
        private const val PREF_SEED_VERSION = "seed_db_version"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        /**
         * Reset database by deleting it. Room will reload seed DB from assets on next access.
         * Useful for testing/debugging.
         */
        fun resetDatabase(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                
                val dbFile = context.getDatabasePath(DB_NAME)
                if (dbFile.exists()) {
                    dbFile.delete()
                    android.util.Log.d("AppDatabase", "Database file deleted")
                }
                
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .remove(PREF_SEED_VERSION)
                    .remove(PREF_SEED_COPIED)
                    .apply()
                
                android.util.Log.d("AppDatabase", "Database reset - will load seed DB from assets on next access")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = context.getDatabasePath(DB_NAME)
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val seedVersion = 4  // Incremented to force re-copy of new seed DB with fixed schema
                val lastCopiedVersion = prefs.getInt(PREF_SEED_VERSION, 0)
                
                android.util.Log.d("AppDatabase", "Getting database instance - seedVersion: $seedVersion, lastCopiedVersion: $lastCopiedVersion, dbExists: ${dbFile.exists()}")
                
                val builder = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                
                // If database doesn't exist or version changed, delete old DB and use seed DB from assets
                if (!dbFile.exists() || lastCopiedVersion != seedVersion) {
                    android.util.Log.d("AppDatabase", "Seed version changed or DB doesn't exist. Deleting old DB and using seed database from assets")
                    // Delete existing database if it exists (to force re-copy with new schema)
                    if (dbFile.exists()) {
                        try {
                            // Close any existing connections first
                            INSTANCE?.close()
                            INSTANCE = null
                            
                            // Delete database file and related files
                            dbFile.delete()
                            File("${dbFile.absolutePath}-wal").delete()
                            File("${dbFile.absolutePath}-shm").delete()
                            File("${dbFile.absolutePath}-journal").delete()
                            android.util.Log.d("AppDatabase", "Deleted existing database file and related files")
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error deleting database file", e)
                            e.printStackTrace()
                        }
                    }
                    builder.createFromAsset(SEED_DB_ASSET_PATH)
                    
                    // Mark as copied after Room loads it
                    prefs.edit()
                        .putInt(PREF_SEED_VERSION, seedVersion)
                        .apply()
                } else {
                    android.util.Log.d("AppDatabase", "Using existing database")
                }
                
                @Suppress("DEPRECATION")
                val instance = try {
                    builder
                        .fallbackToDestructiveMigration() // For development: recreate DB if schema changes
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                // Enable foreign keys
                                db.execSQL("PRAGMA foreign_keys = ON")
                                android.util.Log.d("AppDatabase", "Database onCreate called")
                            }
                            
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                super.onOpen(db)
                                // Enable foreign keys on every connection
                                db.execSQL("PRAGMA foreign_keys = ON")
                                android.util.Log.d("AppDatabase", "Database onOpen called")
                                
                                // Verify seed data was loaded
                                try {
                                    val cursor = db.query("SELECT COUNT(*) FROM Postrojenje")
                                    if (cursor.moveToFirst()) {
                                        val count = cursor.getInt(0)
                                        android.util.Log.d("AppDatabase", "Database opened - Postrojenje count: $count")
                                        
                                        // Also check other tables
                                        val uredajCursor = db.query("SELECT COUNT(*) FROM Uredaj")
                                        if (uredajCursor.moveToFirst()) {
                                            android.util.Log.d("AppDatabase", "Uredaj count: ${uredajCursor.getInt(0)}")
                                        }
                                        uredajCursor.close()
                                        
                                        val poljeCursor = db.query("SELECT COUNT(*) FROM Polje")
                                        if (poljeCursor.moveToFirst()) {
                                            android.util.Log.d("AppDatabase", "Polje count: ${poljeCursor.getInt(0)}")
                                        }
                                        poljeCursor.close()
                                        
                                        val vrstaCursor = db.query("SELECT COUNT(*) FROM VrstaUredaja")
                                        if (vrstaCursor.moveToFirst()) {
                                            android.util.Log.d("AppDatabase", "VrstaUredaja count: ${vrstaCursor.getInt(0)}")
                                        }
                                        vrstaCursor.close()
                                        
                                        val paramCursor = db.query("SELECT COUNT(*) FROM ParametarProvjere")
                                        if (paramCursor.moveToFirst()) {
                                            android.util.Log.d("AppDatabase", "ParametarProvjere count: ${paramCursor.getInt(0)}")
                                        }
                                        paramCursor.close()
                                    }
                                    cursor.close()
                                } catch (e: Exception) {
                                    android.util.Log.e("AppDatabase", "Error checking database counts", e)
                                    e.printStackTrace()
                                }
                            }
                        })
                        .build()
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Error building database", e)
                    e.printStackTrace()
                    throw e
                }
                INSTANCE = instance
                instance
            }
        }
    }
}
