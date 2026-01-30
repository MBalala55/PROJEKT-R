package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.dto.PostrojenjeSummary
import com.example.elektropregled.data.repository.PostrojenjeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class FacilityListUiState(
    val isLoading: Boolean = false,
    val facilities: List<PostrojenjeSummary> = emptyList(),
    val errorMessage: String? = null,
    val isOffline: Boolean = false
)

class FacilityListViewModel(
    private val repository: PostrojenjeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FacilityListUiState())
    val uiState: StateFlow<FacilityListUiState> = _uiState
    
    init {
        // Start observing Flow from local DB (offline-first)
        loadFacilities()
        
        // Trigger background sync if online
        repository.triggerSyncIfOnline(viewModelScope)
    }
    
    fun loadFacilities() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                android.util.Log.d("FacilityListViewModel", "Starting to collect from Flow")
                
                // Collect from a single Flow instance - Room Flows emit immediately when data exists
                repository.getPostrojenjaFlow()
                    .catch { e ->
                        android.util.Log.e("FacilityListViewModel", "Error in Flow", e)
                        e.printStackTrace()
                        emit(emptyList())
                    }
                    .collect { facilities ->
                        android.util.Log.d("FacilityListViewModel", "Received ${facilities.size} facilities from Flow")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            facilities = facilities,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("FacilityListViewModel", "Exception in loadFacilities", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška: ${e.message}",
                    facilities = emptyList()
                )
            }
        }
    }
    
    /**
     * Manually trigger sync with server.
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            repository.syncPostrojenja()
                .onSuccess {
                    // Flow will automatically update UI when DB changes
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Greška pri sinkronizaciji",
                        isOffline = true
                    )
                }
        }
    }
    
    fun isOverdue(lastInspection: String?): Boolean {
        if (lastInspection == null) return true
        
        return try {
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
            val lastDate = LocalDateTime.parse(lastInspection, formatter)
            val now = LocalDateTime.now()
            val monthsAgo = java.time.temporal.ChronoUnit.MONTHS.between(lastDate, now)
            monthsAgo >= 1
        } catch (e: Exception) {
            true // If we can't parse, consider it overdue
        }
    }
}
