package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.database.entity.PregledEntity
import com.example.elektropregled.data.repository.PregledRepository
import com.example.elektropregled.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SyncUiState(
    val isLoading: Boolean = false,
    val pendingCount: Int = 0,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val errorMessage: String? = null,
    val lastSyncResult: SyncResult? = null
)

class SyncViewModel(
    private val repository: PregledRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState
    
    init {
        loadSyncStatus()
    }
    
    fun loadSyncStatus() {
        viewModelScope.launch {
            repository.getAllPregledi().collect { pregledi ->
                val pending = pregledi.count { it.status_sinkronizacije == "PENDING" || it.status_sinkronizacije == "FAILED" }
                val synced = pregledi.count { it.status_sinkronizacije == "SYNCED" }
                val failed = pregledi.count { it.status_sinkronizacije == "FAILED" }
                
                _uiState.value = _uiState.value.copy(
                    pendingCount = pending,
                    syncedCount = synced,
                    failedCount = failed
                )
            }
        }
    }
    
    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = repository.syncPregledi()
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                lastSyncResult = result
            )
            
            when (result) {
                is SyncResult.Success -> {
                    loadSyncStatus()
                }
                is SyncResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = result.message
                    )
                }
            }
        }
    }
}
