package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.dto.ChecklistParametar
import com.example.elektropregled.data.api.dto.ChecklistUredaj
import com.example.elektropregled.data.database.AppDatabase
import com.example.elektropregled.data.database.entity.PregledEntity
import com.example.elektropregled.data.database.entity.StavkaPregledaEntity
import com.example.elektropregled.data.repository.ChecklistRepository
import com.example.elektropregled.data.repository.PregledRepository
import com.example.elektropregled.data.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class ChecklistUiState(
    val isLoading: Boolean = false,
    val uredaji: List<ChecklistUredaj> = emptyList(),
    val errorMessage: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class ChecklistViewModel(
    private val checklistRepository: ChecklistRepository,
    private val pregledRepository: PregledRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChecklistUiState())
    val uiState: StateFlow<ChecklistUiState> = _uiState
    
    private var currentPregledId: Int? = null
    private val parameterValues = mutableMapOf<String, Any?>() // Key: "uredId-paramId", Value: actual value
    
    fun loadChecklist(postrojenjeId: Int, poljeId: Int?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            checklistRepository.getChecklist(postrojenjeId, poljeId)
                .onSuccess { uredaji ->
                    // Initialize with default values
                    uredaji.forEach { uredaj ->
                        uredaj.parametri.forEach { parametar ->
                            val key = "${uredaj.idUred}-${parametar.idParametra}"
                            if (!parameterValues.containsKey(key)) {
                                when (parametar.tipPodataka) {
                                    "BOOLEAN" -> parameterValues[key] = parametar.defaultVrijednostBool ?: true
                                    "NUMERIC" -> parameterValues[key] = parametar.defaultVrijednostNum
                                    "TEXT" -> parameterValues[key] = parametar.defaultVrijednostTxt
                                }
                            }
                        }
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        uredaji = uredaji
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Greška pri učitavanju checklistte"
                    )
                }
        }
    }
    
    fun updateParameterValue(uredajId: Int, parametarId: Int, value: Any?) {
        val key = "$uredajId-$parametarId"
        parameterValues[key] = value
    }
    
    fun getParameterValue(uredajId: Int, parametarId: Int): Any? {
        val key = "$uredajId-$parametarId"
        return parameterValues[key]
    }
    
    fun startInspection(postrojenjeId: Int, napomena: String? = null) {
        viewModelScope.launch {
            try {
                val pregled = pregledRepository.createPregled(postrojenjeId, napomena)
                currentPregledId = pregled.id_preg
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Greška pri kreiranju pregleda: ${e.message}"
                )
            }
        }
    }
    
    suspend fun getCurrentPregled(pregledId: Int): PregledEntity? {
        return pregledRepository.getAllPregledi()
            .first()
            .find { it.id_preg == pregledId }
    }
    
    fun saveInspection() {
        val pregledId = currentPregledId ?: run {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Pregled nije započet"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null)
            
            try {
                val current = getCurrentPregled(pregledId)
                if (current == null) {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        errorMessage = "Pregled nije pronađen"
                    )
                    return@launch
                }
                
                val stavke = parameterValues.mapNotNull { (key, value) ->
                    val (uredId, paramId) = key.split("-").map { it.toInt() }
                    
                    // Find parameter to get type
                    val parametar = _uiState.value.uredaji
                        .flatMap { it.parametri }
                        .find { it.idParametra == paramId }
                    
                    if (parametar == null) return@mapNotNull null
                    
                    val now = java.time.LocalDateTime.now().toString()
                    val lokalniId = UUID.randomUUID().toString()
                    
                    when (parametar.tipPodataka) {
                        "BOOLEAN" -> {
                            val boolValue = value as? Boolean ?: true
                            StavkaPregledaEntity(
                                lokalni_id = lokalniId,
                                vrijednost_bool = if (boolValue) 1 else 0,
                                vrijednost_num = null,
                                vrijednost_txt = null,
                                vrijeme_unosa = now,
                                id_preg = pregledId,
                                id_ured = uredId,
                                id_parametra = paramId
                            )
                        }
                        "NUMERIC" -> {
                            val numValue = value as? Double
                            if (numValue == null && parametar.obavezan) return@mapNotNull null
                            StavkaPregledaEntity(
                                lokalni_id = lokalniId,
                                vrijednost_bool = null,
                                vrijednost_num = numValue,
                                vrijednost_txt = null,
                                vrijeme_unosa = now,
                                id_preg = pregledId,
                                id_ured = uredId,
                                id_parametra = paramId
                            )
                        }
                        "TEXT" -> {
                            val txtValue = value as? String
                            if (txtValue.isNullOrBlank() && parametar.obavezan) return@mapNotNull null
                            StavkaPregledaEntity(
                                lokalni_id = lokalniId,
                                vrijednost_bool = null,
                                vrijednost_num = null,
                                vrijednost_txt = txtValue,
                                vrijeme_unosa = now,
                                id_preg = pregledId,
                                id_ured = uredId,
                                id_parametra = paramId
                            )
                        }
                        else -> null
                    }
                }
                
                pregledRepository.savePregledLocally(current, stavke)
                pregledRepository.finishPregled(pregledId)
                
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Greška pri spremanju: ${e.message}"
                )
            }
        }
    }
}
