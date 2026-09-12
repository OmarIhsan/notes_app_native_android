package com.mal5odha.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mal5odha.core.data.models.UserSession
import com.mal5odha.core.data.preferences.ThemeMode
import com.mal5odha.core.data.preferences.UserPreferencesRepository
import com.mal5odha.core.data.services.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authService: AuthService
) : ViewModel() {

    val currentSession: StateFlow<UserSession?> = authService.currentSession

    fun logout() {
        viewModelScope.launch {
            authService.logout()
        }
    }

    val themeMode: StateFlow<ThemeMode> = userPreferencesRepository.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val isDarkMode: StateFlow<Boolean> = themeMode
        .map { mode ->
            when (mode) {
                ThemeMode.SYSTEM -> false
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeMode(mode)
        }
    }

    fun toggleThemeMode() {
        viewModelScope.launch {
            userPreferencesRepository.toggleThemeMode()
        }
    }

    fun toggleTheme(isDark: Boolean) {
        setThemeMode(if (isDark) ThemeMode.DARK else ThemeMode.LIGHT)
    }
}
