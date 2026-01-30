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
    version = 2,
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
        
        private const val DB_NAME = "elektropregled_database"
        private const val SEED_DB_NAME = "seed.db"
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // IMPORTANT: Room's createFromAsset() only copies if database doesn't exist
                // So we must delete it BEFORE Room tries to open it
                val dbFile = context.getDatabasePath(DB_NAME)
                val dbDir = dbFile.parentFile
                
                android.util.Log.d("AppDatabase", "Checking database at: ${dbFile.absolutePath}")
                android.util.Log.d("AppDatabase", "Database exists: ${dbFile.exists()}")
                
                if (dbFile.exists()) {
                    var shouldDelete = false
                    var reason = ""
                    
                    try {
                        // Check if database has data and correct version by opening it temporarily
                        val tempConn = android.database.sqlite.SQLiteDatabase.openDatabase(
                            dbFile.absolutePath,
                            null,
                            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
                        )
                        
                        // Check user_version (must match Room database version = 1)
                        val versionCursor = tempConn.rawQuery("PRAGMA user_version", null)
                        val userVersion = if (versionCursor.moveToFirst()) versionCursor.getInt(0) else 0
                        versionCursor.close()
                        
                        android.util.Log.d("AppDatabase", "Database user_version: $userVersion (expected: 1)")
                        
                        // Check if Polje table has any data
                        val cursor = tempConn.rawQuery("SELECT COUNT(*) FROM Polje", null)
                        val count = if (cursor.moveToFirst()) cursor.getInt(0) else 0
                        cursor.close()
                        tempConn.close()
                        
                        android.util.Log.d("AppDatabase", "Polje table count: $count")
                        
                        // Delete if wrong version OR empty
                        if (userVersion != 2 && userVersion != 1) {
                            shouldDelete = true
                            reason = "wrong version ($userVersion, expected 1 or 2)"
                        } else if (count == 0) {
                            shouldDelete = true
                            reason = "empty (0 polja)"
                        } else {
                            android.util.Log.d("AppDatabase", "Database is valid: version=$userVersion, polja=$count")
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AppDatabase", "Could not check database: ${e.message}. Will delete to be safe.")
                        shouldDelete = true
                        reason = "check failed: ${e.message}"
                    }
                    
                    if (shouldDelete) {
                        android.util.Log.d("AppDatabase", "Deleting database because: $reason")
                        
                        // Close any existing connections first
                        try {
                            INSTANCE?.close()
                            INSTANCE = null
                        } catch (e: Exception) {
                            // Ignore
                        }
                        
                        // Delete database and related files
                        var deleted = false
                        try {
                            // Try multiple times in case file is locked
                            for (i in 1..3) {
                                if (dbFile.delete()) {
                                    deleted = true
                                    break
                                }
                                android.util.Log.d("AppDatabase", "Delete attempt $i failed, retrying...")
                                Thread.sleep(100)
                            }
                            
                            if (deleted) {
                                android.util.Log.d("AppDatabase", "Database file deleted successfully")
                            } else {
                                android.util.Log.w("AppDatabase", "Could not delete database file (may be locked)")
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("AppDatabase", "Error deleting database: ${e.message}")
                        }
                        
                        // Delete related files
                        val journalFile = java.io.File(dbFile.absolutePath + "-journal")
                        val walFile = java.io.File(dbFile.absolutePath + "-wal")
                        val shmFile = java.io.File(dbFile.absolutePath + "-shm")
                        journalFile.delete()
                        walFile.delete()
                        shmFile.delete()
                        
                        android.util.Log.d("AppDatabase", "Database cleanup complete. Room will copy seed.db on next open.")
                        
                        // Verify file is actually deleted
                        if (dbFile.exists()) {
                            android.util.Log.e("AppDatabase", "WARNING: Database file still exists after deletion attempt!")
                        } else {
                            android.util.Log.d("AppDatabase", "Confirmed: Database file deleted successfully")
                        }
                    }
                } else {
                    android.util.Log.d("AppDatabase", "Database does not exist. Room will create it and copy from seed.db")
                }
                
                // Verify seed.db exists in assets
                try {
                    val assetManager = context.assets
                    val assetPath = "database/$SEED_DB_NAME"
                    val inputStream = assetManager.open(assetPath)
                    inputStream.close()
                    android.util.Log.d("AppDatabase", "seed.db found in assets at: $assetPath")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "ERROR: seed.db not found in assets at database/$SEED_DB_NAME: ${e.message}")
                }
                
                // Migration from version 1 to 2: Add zadnji_pregled column to Postrojenje table
                val MIGRATION_1_2 = object : Migration(1, 2) {
                    override fun migrate(database: SupportSQLiteDatabase) {
                        database.execSQL("ALTER TABLE Postrojenje ADD COLUMN zadnji_pregled TEXT")
                        android.util.Log.d("AppDatabase", "Migration 1->2: Added zadnji_pregled column to Postrojenje")
                    }
                }
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Enable foreign keys
                            db.execSQL("PRAGMA foreign_keys = ON")
                            android.util.Log.d("AppDatabase", "onCreate called - seed.db should have been copied")
                            
                            // Verify data was loaded from seed.db
                            try {
                                val cursor = db.query("SELECT COUNT(*) FROM Polje", emptyArray())
                                if (cursor.moveToFirst()) {
                                    val count = cursor.getInt(0)
                                    android.util.Log.d("AppDatabase", "Polje table has $count rows after onCreate")
                                    if (count == 0) {
                                        android.util.Log.e("AppDatabase", "ERROR: Polje table is empty! seed.db was NOT copied correctly.")
                                        android.util.Log.e("AppDatabase", "This usually means the database file already existed when Room tried to copy.")
                                    } else {
                                        android.util.Log.d("AppDatabase", "SUCCESS: seed.db data loaded correctly!")
                                    }
                                }
                                cursor.close()
                            } catch (e: Exception) {
                                android.util.Log.e("AppDatabase", "Error checking Polje count in onCreate: ${e.message}")
                            }
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Enable foreign keys on every connection
                            db.execSQL("PRAGMA foreign_keys = ON")
                            
                            // Log data count for debugging
                            try {
                                val cursor = db.query("SELECT COUNT(*) FROM Polje", emptyArray())
                                if (cursor.moveToFirst()) {
                                    val count = cursor.getInt(0)
                                    android.util.Log.d("AppDatabase", "Database opened - Polje table has $count rows")
                                }
                                cursor.close()
                            } catch (e: Exception) {
                                // Ignore errors in logging
                            }
                        }
                    })
                    .createFromAsset("database/$SEED_DB_NAME")
                    .fallbackToDestructiveMigration() // Only if schema changes
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
