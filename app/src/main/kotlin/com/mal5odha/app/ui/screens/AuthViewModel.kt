package com.mal5odha.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.UserSession
import com.mal5odha.core.data.services.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val successMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(private val authService: AuthService) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    val isLoggedIn: StateFlow<Boolean> = authService.isLoggedIn
    val currentSession: StateFlow<UserSession?> = authService.currentSession

    fun continueAsGuest() {
        authService.continueAsGuest()
        _uiState.value = AuthUiState(successMessage = "Guest session started")
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState(error = "Fields cannot be empty")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authService.login(email, pass)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(successMessage = "Logged in successfully")
            } else {
                _uiState.value =
                        AuthUiState(error = result.exceptionOrNull()?.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, pass: String, passConfirm: String) {
        if (email.isBlank() || pass.isBlank()) {
            _uiState.value = AuthUiState(error = "Fields cannot be empty")
            return
        }
        if (pass != passConfirm) {
            _uiState.value = AuthUiState(error = "Passwords do not match")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            val result = authService.register(email, pass)
            if (result.isSuccess) {
                _uiState.value = AuthUiState(successMessage = "Account created successfully")
            } else {
                _uiState.value =
                        AuthUiState(
                                error = result.exceptionOrNull()?.message ?: "Registration failed"
                        )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
