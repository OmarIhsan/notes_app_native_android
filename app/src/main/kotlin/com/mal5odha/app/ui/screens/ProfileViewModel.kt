package com.mal5odha.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.UserSession
import com.mal5odha.core.data.services.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(private val authService: AuthService) : ViewModel() {

    val currentSession: StateFlow<UserSession?> = authService.currentSession

    fun logout() {
        viewModelScope.launch { authService.logout() }
    }
}
