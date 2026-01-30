package com.example.elektropregled

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.elektropregled.data.EncryptedTokenStorage
import com.example.elektropregled.data.TokenStorage
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ElektropregledApplication : Application() {
    
    val database by lazy { AppDatabase.getDatabase(this) }
    val tokenStorage: TokenStorage by lazy { EncryptedTokenStorage(this) }
    
    val postrojenjeRepository by lazy { PostrojenjeRepository(database, tokenStorage) }
    val checklistRepository by lazy { ChecklistRepository(database, tokenStorage) }
    val pregledRepository by lazy { PregledRepository(database, tokenStorage) }
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var wasOffline = false
    private var isSyncing = false
    
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
        
        // Set up network callback to auto-sync when connection is restored
        setupNetworkCallback()
    }
    
    private fun setupNetworkCallback() {
        val connectivityManager = getSystemService(ConnectivityManager::class.java)
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()
        
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d("ElektropregledApplication", "Network available - checking if sync needed")
                
                // Check if we have pending pregledi to sync
                if (!isSyncing && wasOffline) {
                    isSyncing = true
                    applicationScope.launch {
                        try {
                            if (tokenStorage.isTokenValid()) {
                                val pregledi = pregledRepository.getAllPregledi().first()
                                val hasPending = pregledi.any { 
                                    it.status_sinkronizacije == "PENDING" || 
                                    it.status_sinkronizacije == "FAILED" 
                                }
                                
                                if (hasPending) {
                                    Log.d("ElektropregledApplication", "Connection restored - auto-syncing pending pregledi")
                                    val result = pregledRepository.syncPregledi()
                                    when (result) {
                                        is SyncResult.Success -> {
                                            Log.d("ElektropregledApplication", "Auto-sync successful: ${result.synced} pregledi synced")
                                        }
                                        is SyncResult.Error -> {
                                            Log.d("ElektropregledApplication", "Auto-sync failed: ${result.message}")
                                        }
                                    }
                                }
                            }
                            wasOffline = false
                        } catch (e: Exception) {
                            Log.e("ElektropregledApplication", "Error in auto-sync", e)
                        } finally {
                            isSyncing = false
                        }
                    }
                }
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d("ElektropregledApplication", "Network lost")
                wasOffline = true
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                
                if (hasInternet && wasOffline) {
                    // Connection restored - trigger sync
                    onAvailable(network)
                } else if (!hasInternet) {
                    wasOffline = true
                }
            }
        }
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }
    
    /**
     * Manually trigger sync (e.g., when app comes to foreground)
     */
    fun triggerAutoSync() {
        if (isSyncing) {
            Log.d("ElektropregledApplication", "Sync already in progress, skipping")
            return
        }
        
        isSyncing = true
        applicationScope.launch {
            try {
                if (tokenStorage.isTokenValid()) {
                    val pregledi = pregledRepository.getAllPregledi().first()
                    val hasPending = pregledi.any { 
                        it.status_sinkronizacije == "PENDING" || 
                        it.status_sinkronizacije == "FAILED" 
                    }
                    
                    if (hasPending) {
                        Log.d("ElektropregledApplication", "Triggering manual sync")
                        val result = pregledRepository.syncPregledi()
                        when (result) {
                            is SyncResult.Success -> {
                                Log.d("ElektropregledApplication", "Manual sync successful: ${result.synced} pregledi synced")
                            }
                            is SyncResult.Error -> {
                                Log.d("ElektropregledApplication", "Manual sync failed: ${result.message}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ElektropregledApplication", "Error in manual sync", e)
            } finally {
                isSyncing = false
            }
        }
    }
}
