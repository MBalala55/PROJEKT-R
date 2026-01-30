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
    val errorMessage: String? = null
)

class FacilityListViewModel(
    private val repository: PostrojenjeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FacilityListUiState())
    val uiState: StateFlow<FacilityListUiState> = _uiState
    
    fun loadFacilities() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Load facilities and observe for updates
                repository.getPostrojenjaFlow()
                    .catch { e ->
                        e.printStackTrace()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = e.message ?: "Greška pri učitavanju postrojenja"
                        )
                    }
                    .collect { facilities ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            facilities = facilities,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                e.printStackTrace() // Log unexpected exceptions
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Neočekivana greška: ${e.message}"
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
