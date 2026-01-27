package com.example.elektropregled

import android.app.Application
import android.util.Log
import com.example.elektropregled.data.EncryptedTokenStorage
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.repository.*

class ElektropregledApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(this) }
    
    val postrojenjeRepository by lazy { PostrojenjeRepository(database, tokenStorage) }
    val checklistRepository by lazy { ChecklistRepository(database, tokenStorage) }
    val pregledRepository by lazy { PregledRepository(database, tokenStorage) }
    
    override fun onCreate() {
        super.onCreate()
        
        // Set up global exception handler to catch crashes
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            Log.e("Elektropregled", "Uncaught exception in thread ${thread.name}", exception)
            exception.printStackTrace()
            // Call the default handler
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            defaultHandler?.uncaughtException(thread, exception)
        }
    }
}
