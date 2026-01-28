package com.example.elektropregled.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.elektropregled.data.api.ApiClient
import com.example.elektropregled.data.api.dto.LoginRequest
import com.example.elektropregled.data.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false
)

class LoginViewModel(
    private val tokenStorage: TokenStorage
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState
    
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Molimo unesite korisničko ime i lozinku"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                val response = ApiClient.apiService.login(LoginRequest(username, password))
                
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!
                    tokenStorage.saveToken(loginResponse.access_token, loginResponse.expires_in)
                    tokenStorage.saveUserId(loginResponse.user_id)
                    tokenStorage.saveUsername(loginResponse.username)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                } else {
                    val errorMsg = when (response.code()) {
                        401 -> "Neispravno korisničko ime ili lozinka"
                        400 -> "Nedostaju podaci"
                        else -> "Greška pri prijavi: ${response.code()}"
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace() // Log the full exception
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Greška pri prijavi: ${e.message ?: e.javaClass.simpleName}"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}
