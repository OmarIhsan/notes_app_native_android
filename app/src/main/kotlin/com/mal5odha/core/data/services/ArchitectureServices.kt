package com.mal5odha.core.data.services

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.mal5odha.core.data.models.UserSession
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

interface AppPreferencesService {
    suspend fun saveTheme(isDark: Boolean)
    fun isDarkMode(): Flow<Boolean>
}

@Singleton
class AppPreferencesServiceImpl
@Inject
constructor(@dagger.hilt.android.qualifiers.ApplicationContext private val context: Context) :
        AppPreferencesService {

    private val themeKey = booleanPreferencesKey("is_dark_mode")

    override suspend fun saveTheme(isDark: Boolean) {
        context.dataStore.edit { preferences -> preferences[themeKey] = isDark }
    }

    override fun isDarkMode(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[themeKey] ?: false // Default to light mode (false)
        }
    }
}

interface ErrorHandlingService {
    val currentError: StateFlow<Throwable?>
    fun logError(error: Throwable)
    fun recoverSession()
    fun clearError()
}

@Singleton
class ErrorHandlingServiceImpl
@Inject
constructor(
        private val authService: AuthService,
        private val appPreferencesService: AppPreferencesService
) : ErrorHandlingService {

    private val _currentError = MutableStateFlow<Throwable?>(null)
    override val currentError: StateFlow<Throwable?> = _currentError.asStateFlow()

    override fun logError(error: Throwable) {
        // In a real app, send to Crashlytics/Sentry
        // For local error recovery dialog:
        _currentError.value = error
    }

    override fun recoverSession() {
        // Attempt aggressive recovery logic
        clearError()
    }

    override fun clearError() {
        _currentError.value = null
    }
}

interface DataMigrationService {
    fun migrateDataIfNeeded()
}

@Singleton
class DataMigrationServiceImpl @Inject constructor() : DataMigrationService {
    override fun migrateDataIfNeeded() {
        // TODO: Implement
    }
}

interface SecureStorageService {
    fun saveToken(token: String)
    fun getToken(): String?
    fun clearToken()

    fun saveUserSession(session: UserSession)
    fun getUserSession(): UserSession?
    fun clearUserSession()
}

@Singleton
class SecureStorageServiceImpl
@Inject
constructor(
        @dagger.hilt.android.qualifiers.ApplicationContext
        private val context: android.content.Context
) : SecureStorageService {

    private val prefs: android.content.SharedPreferences by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            android.util.Log.e("SecureStorage", "AEAD / Keystore authentication failure detected, executing complete keyset purge", e)
            purgeEncryptedPrefsAndKeyset()
            try {
                createEncryptedPrefs()
            } catch (fallbackEx: Exception) {
                android.util.Log.e("SecureStorage", "Fallback to private SharedPreferences after Keystore failure", fallbackEx)
                context.getSharedPreferences("secure_auth_prefs_fallback", android.content.Context.MODE_PRIVATE)
            }
        }
    }

    private fun purgeEncryptedPrefsAndKeyset() {
        try {
            // 1. Purge encrypted preference XML and all Google Tink keyset XML files
            val sharedPrefsDir = java.io.File(context.filesDir.parent, "shared_prefs")
            if (sharedPrefsDir.exists() && sharedPrefsDir.isDirectory) {
                val targets = listOf(
                    "secure_auth_prefs.xml",
                    "secret_shared_prefs.xml",
                    "__androidx_security_crypto_encrypted_file_keyset__.xml",
                    "__androidx_security_crypto_encrypted_prefs_key_keyset__.xml",
                    "__androidx_security_crypto_encrypted_prefs_value_keyset__.xml"
                )
                for (target in targets) {
                    val targetFile = java.io.File(sharedPrefsDir, target)
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                }
                // Also sweep any dynamically named Tink keyset files
                sharedPrefsDir.listFiles()?.forEach { file ->
                    if (file.name.contains("keyset") || file.name.startsWith("__androidx_security_crypto_")) {
                        file.delete()
                    }
                }
            }

            // 2. Invalidate and remove master key alias in AndroidKeyStore
            val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val masterKeyAlias = androidx.security.crypto.MasterKey.DEFAULT_MASTER_KEY_ALIAS
            if (keyStore.containsAlias(masterKeyAlias)) {
                keyStore.deleteEntry(masterKeyAlias)
            }
        } catch (purgeError: Exception) {
            android.util.Log.e("SecureStorage", "Error while purging corrupted Keystore keyset entries", purgeError)
        }
    }

    private fun createEncryptedPrefs(): android.content.SharedPreferences {
        val masterKey = androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
            .build()
        return androidx.security.crypto.EncryptedSharedPreferences.create(
            context,
            "secure_auth_prefs",
            masterKey,
            androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun saveToken(token: String) {
        prefs.edit().putString("AUTH_TOKEN", token).apply()
    }

    override fun getToken(): String? {
        return prefs.getString("AUTH_TOKEN", null)
    }

    override fun clearToken() {
        prefs.edit().remove("AUTH_TOKEN").apply()
    }

    override fun saveUserSession(session: UserSession) {
        prefs.edit()
            .putString("SESSION_USER_ID", session.userId)
            .putString("SESSION_EMAIL", session.email)
            .putString("SESSION_DISPLAY_NAME", session.displayName)
            .putBoolean("SESSION_IS_GUEST", session.isGuest)
            .putString("SESSION_TOKEN", session.token)
            .putLong("SESSION_CREATED_AT", session.createdAt)
            .apply()

        if (session.token != null) {
            saveToken(session.token)
        } else {
            clearToken()
        }
    }

    override fun getUserSession(): UserSession? {
        val userId = prefs.getString("SESSION_USER_ID", null)
        if (userId != null) {
            val email = prefs.getString("SESSION_EMAIL", null)
            val displayName = prefs.getString("SESSION_DISPLAY_NAME", "User") ?: "User"
            val isGuest = prefs.getBoolean("SESSION_IS_GUEST", false)
            val token = prefs.getString("SESSION_TOKEN", null) ?: getToken()
            val createdAt = prefs.getLong("SESSION_CREATED_AT", System.currentTimeMillis())
            return UserSession(
                userId = userId,
                email = email,
                displayName = displayName,
                isGuest = isGuest,
                token = token,
                createdAt = createdAt
            )
        }

        // Backward compatibility fallback for existing installs with only AUTH_TOKEN
        val legacyToken = getToken()
        if (legacyToken != null) {
            return UserSession(
                userId = "usr_legacy",
                email = "user@mal5odha.app",
                displayName = "Student",
                isGuest = false,
                token = legacyToken,
                createdAt = System.currentTimeMillis()
            )
        }

        return null
    }

    override fun clearUserSession() {
        prefs.edit()
            .remove("SESSION_USER_ID")
            .remove("SESSION_EMAIL")
            .remove("SESSION_DISPLAY_NAME")
            .remove("SESSION_IS_GUEST")
            .remove("SESSION_TOKEN")
            .remove("SESSION_CREATED_AT")
            .remove("AUTH_TOKEN")
            .apply()
    }
}
