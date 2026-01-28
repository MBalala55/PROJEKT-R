package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.repository.PostrojenjeRepository
import com.example.elektropregled.data.repository.PregledRepository
import com.example.elektropregled.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FieldListUiState(
    val isLoading: Boolean = false,
    val fields: List<PoljeDto> = emptyList(),
    val errorMessage: String? = null,
    val reviewedFieldsCount: Int = 0,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val reviewedFieldIds: Set<Int> = emptySet()
)

class FieldListViewModel(
    private val repository: PostrojenjeRepository,
    private val pregledRepository: PregledRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(FieldListUiState())
    val uiState: StateFlow<FieldListUiState> = _uiState
    
    private var currentPregledId: Int? = null
    private var currentPostrojenjeId: Int? = null
    private var totalFieldsCount: Int = 0
    private var checklistViewModel: ChecklistViewModel? = null
    private val reviewedFieldIds = mutableSetOf<Int>()
    private val countedFieldIds = mutableSetOf<Int>()
    
    fun setChecklistViewModel(viewModel: ChecklistViewModel) {
        this.checklistViewModel = viewModel
    }
    
    fun loadFields(postrojenjeId: Int) {
        // Don't reload if already loaded for the same facility
        if (currentPostrojenjeId == postrojenjeId && _uiState.value.fields.isNotEmpty()) {
            return
        }
        
        // Reset counter when loading a new facility
        currentPostrojenjeId = postrojenjeId
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Start inspection for this facility
            val pregled = pregledRepository.createPregled(postrojenjeId, null)
            currentPregledId = pregled.id_preg
            
            repository.getPolja(postrojenjeId)
                .onSuccess { fields ->
                    totalFieldsCount = fields.size
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        fields = fields,
                        reviewedFieldsCount = 0
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
    
    fun saveCurrentFieldReview() {
        // Save the current field review before navigating to next field
        checklistViewModel?.saveFieldReview()
    }
    
    fun onFieldReviewed() {
        val current = _uiState.value.reviewedFieldsCount + 1
        _uiState.value = _uiState.value.copy(reviewedFieldsCount = current)
    }
    
    fun markFieldAsReviewed(fieldId: Int) {
        reviewedFieldIds.add(fieldId)
        
        // Povećaj brojač samo ako je prvi put da se polje pregledava
        if (!countedFieldIds.contains(fieldId)) {
            countedFieldIds.add(fieldId)
            val current = _uiState.value.reviewedFieldsCount + 1
            _uiState.value = _uiState.value.copy(
                reviewedFieldsCount = current,
                reviewedFieldIds = reviewedFieldIds.toSet()
            )
        } else {
            // Samo ažuriraj vizualni indikator bez povećanja brojača
            _uiState.value = _uiState.value.copy(reviewedFieldIds = reviewedFieldIds.toSet())
        }
    }
    
    fun finishInspection() {
        val pregledId = currentPregledId ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Pregled nije započet"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            
            try {
                // Save the last field review before finishing
                checklistViewModel?.saveFieldReview()
                
                // Mark inspection as finished locally
                pregledRepository.finishPregled(pregledId)
                
                // Sync to server
                val syncResult = pregledRepository.syncPregledi()
                
                when (syncResult) {
                    is SyncResult.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveSuccess = true
                        )
                    }
                    is SyncResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            errorMessage = "Greška pri sinhronizaciji: ${syncResult.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Greška pri završavanju pregleda: ${e.message}"
                )
            }
        }
    }
    
    fun resetSaveSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = false)
    }
}
