package com.mal5odha.core.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

interface UserPreferencesRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun toggleThemeMode()
}

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : UserPreferencesRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val themeMode: Flow<ThemeMode> = context.userPreferencesDataStore.data.map { preferences ->
        val rawMode = preferences[PreferencesKeys.THEME_MODE]
        if (rawMode != null) {
            try {
                ThemeMode.valueOf(rawMode)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        } else {
            ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    override suspend fun toggleThemeMode() {
        val current = themeMode.first()
        val next = when (current) {
            ThemeMode.SYSTEM -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
        }
        setThemeMode(next)
    }
}
