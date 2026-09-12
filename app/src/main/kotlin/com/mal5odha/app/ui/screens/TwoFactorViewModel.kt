package com.mal5odha.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.services.SecureStorageService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TwoFactorUiState(
    val step: Int = 1, // 1: Info, 2: Key Generation, 3: Verification, 4: Success
    val secretKey: String? = null,
    val isVerifying: Boolean = false,
    val verificationError: String? = null,
    val isEnabled: Boolean = false
)

@HiltViewModel
class TwoFactorViewModel @Inject constructor(
    private val secureStorageService: SecureStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TwoFactorUiState())
    val uiState: StateFlow<TwoFactorUiState> = _uiState

    init {
        // Mock: check if already enabled
        viewModelScope.launch {
             _uiState.value = _uiState.value.copy(isEnabled = secureStorageService.getToken() != null)
        }
    }

    fun nextStep() {
        val next = _uiState.value.step + 1
        if (next == 2) generateKey()
        _uiState.value = _uiState.value.copy(step = next, verificationError = null)
    }

    private fun generateKey() {
        viewModelScope.launch {
            delay(1000) // Mock latency
            _uiState.value = _uiState.value.copy(secretKey = "JBSWY3DPEHPK3PXP") // Mock Base32 key
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isVerifying = true, verificationError = null)
            delay(1500)
            if (code == "123456") { // Mock "correct" code
                _uiState.value = _uiState.value.copy(step = 4, isVerifying = false, isEnabled = true)
            } else {
                _uiState.value = _uiState.value.copy(isVerifying = false, verificationError = "Invalid verification code.")
            }
        }
    }

    fun reset() {
        _uiState.value = TwoFactorUiState()
    }
}
