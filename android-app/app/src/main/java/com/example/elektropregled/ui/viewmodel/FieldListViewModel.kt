package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.dto.PoljeDto
import com.example.elektropregled.data.repository.PostrojenjeRepository
import com.example.elektropregled.data.repository.PregledRepository
import com.example.elektropregled.data.repository.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
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
    
    fun getCurrentPregledId(): Int? {
        return currentPregledId
    }
    
    fun loadFields(postrojenjeId: Int) {
        android.util.Log.d("FieldListViewModel", "loadFields called with postrojenjeId=$postrojenjeId, current=$currentPostrojenjeId, currentPregledId=$currentPregledId")
        
        // Don't reload if already loaded for the same facility
        if (currentPostrojenjeId == postrojenjeId && _uiState.value.fields.isNotEmpty()) {
            android.util.Log.d("FieldListViewModel", "Skipping reload for same facility")
            return
        }
        
        // Reset counter when loading a new facility
        currentPostrojenjeId = postrojenjeId
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Start inspection for this facility ONLY if we don't already have one
            if (currentPregledId == null || currentPostrojenjeId != postrojenjeId) {
                val pregled = pregledRepository.createPregled(postrojenjeId, null)
                currentPregledId = pregled.id_preg
            }
            
            // Trigger background sync if online (non-blocking)
            repository.triggerSyncPoljaIfOnline(viewModelScope, postrojenjeId)
            
            // Collect from Flow - Room Flows emit immediately when data exists
            try {
                android.util.Log.d("FieldListViewModel", "Starting to collect from getPoljaFlow")
                
                repository.getPoljaFlow(postrojenjeId)
                    .catch { e ->
                        android.util.Log.e("FieldListViewModel", "Error in getPoljaFlow", e)
                        e.printStackTrace()
                        emit(emptyList())
                    }
                    .collect { fields ->
                        totalFieldsCount = fields.size
                        android.util.Log.d("FieldListViewModel", "Received ${fields.size} polja from Flow: ${fields.map { "${it.idPolje}:${it.nazPolje}" }}")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            fields = fields,
                            reviewedFieldsCount = _uiState.value.reviewedFieldsCount,
                            errorMessage = null
                        )
                    }
            } catch (e: Exception) {
                android.util.Log.e("FieldListViewModel", "Exception in loadFields", e)
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška: ${e.message}",
                    fields = emptyList()
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
        android.util.Log.d("FieldListViewModel", "markFieldAsReviewed called with fieldId=$fieldId, already counted=${countedFieldIds.contains(fieldId)}")
        
        reviewedFieldIds.add(fieldId)
        
        // Povećaj brojač samo ako je prvi put da se polje pregledava
        if (!countedFieldIds.contains(fieldId)) {
            countedFieldIds.add(fieldId)
            val current = _uiState.value.reviewedFieldsCount + 1
            android.util.Log.d("FieldListViewModel", "Incrementing count to $current")
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
        android.util.Log.d("FieldListViewModel", "finishInspection called, currentPregledId=$currentPregledId")
        
        val pregledId = currentPregledId ?: run {
            android.util.Log.d("FieldListViewModel", "Pregled nije započet!")
            _uiState.value = _uiState.value.copy(
                errorMessage = "Pregled nije započet"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            
            try {
                android.util.Log.d("FieldListViewModel", "Spremam zadnje polje...")
                // Save the last field review before finishing
                checklistViewModel?.saveFieldReview()
                
                // Give a small delay to ensure stavke are committed to database
                kotlinx.coroutines.delay(100)
                
                android.util.Log.d("FieldListViewModel", "Završavam pregled ID: $pregledId")
                // Mark inspection as finished locally (status remains PENDING for offline sync)
                pregledRepository.finishPregled(pregledId)
                
                // Another small delay to ensure finishPregled is committed
                kotlinx.coroutines.delay(100)
                
                android.util.Log.d("FieldListViewModel", "Pokrećem sinkronizaciju...")
                // Try to sync to server (non-blocking - will sync automatically when online)
                val syncResult = pregledRepository.syncPregledi()
                android.util.Log.d("FieldListViewModel", "Sync rezultat: $syncResult")
                
                when (syncResult) {
                    is SyncResult.Success -> {
                        android.util.Log.d("FieldListViewModel", "Sync uspješan, resetiram currentPregledId")
                        // Reset pregled state after successful sync
                        currentPregledId = null
                        reviewedFieldIds.clear()
                        countedFieldIds.clear()
                        
                        _uiState.value = _uiState.value.copy(
                            isSaving = false,
                            saveSuccess = true,
                            reviewedFieldsCount = 0,
                            reviewedFieldIds = emptySet()
                        )
                    }
                    is SyncResult.Error -> {
                        // Check if error is due to network/offline
                        val errorMsg = syncResult.message.lowercase()
                        val isOfflineError = errorMsg.contains("timeout") ||
                                errorMsg.contains("konekcij") ||
                                errorMsg.contains("connection") ||
                                errorMsg.contains("network") ||
                                errorMsg.contains("internet") ||
                                errorMsg.contains("unable to resolve") ||
                                errorMsg.contains("resolve host") ||
                                errorMsg.contains("hostname") ||
                                errorMsg.contains("unknownhost") ||
                                errorMsg.contains("no address associated") ||
                                errorMsg.contains("socket") ||
                                errorMsg.contains("connectexception") ||
                                errorMsg.contains("ioexception")
                        
                        if (isOfflineError) {
                            // Offline mode - pregled is saved locally with PENDING status
                            // It will sync automatically when connection is restored
                            android.util.Log.d("FieldListViewModel", "Offline mode detected - pregled saved locally, will sync when online")
                            currentPregledId = null
                            reviewedFieldIds.clear()
                            countedFieldIds.clear()
                            
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                saveSuccess = true, // Show success even offline
                                reviewedFieldsCount = 0,
                                reviewedFieldIds = emptySet()
                            )
                        } else {
                            // Other error (e.g., token invalid, server error)
                            android.util.Log.d("FieldListViewModel", "Sync neuspješan: ${syncResult.message}, resetiram currentPregledId")
                            currentPregledId = null
                            reviewedFieldIds.clear()
                            countedFieldIds.clear()
                            
                            _uiState.value = _uiState.value.copy(
                                isSaving = false,
                                errorMessage = "Greška pri sinhronizaciji: ${syncResult.message}",
                                reviewedFieldsCount = 0,
                                reviewedFieldIds = emptySet()
                            )
                        }
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
