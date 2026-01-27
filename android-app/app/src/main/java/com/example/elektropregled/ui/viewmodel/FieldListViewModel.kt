package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.repository.PostrojenjeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FieldListUiState(
    val isLoading: Boolean = false,
    val fields: List<PoljeDto> = emptyList(),
    val errorMessage: String? = null
)

class FieldListViewModel(
    private val repository: PostrojenjeRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FieldListUiState())
    val uiState: StateFlow<FieldListUiState> = _uiState
    
    fun loadFields(postrojenjeId: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            repository.getPolja(postrojenjeId)
                .onSuccess { fields ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fields = fields
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Greška pri učitavanju polja"
                    )
                }
        }
    }
}
