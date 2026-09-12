package com.mal5odha.core.data.services

import com.mal5odha.core.data.models.UserSession
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface AuthService {
    val isLoggedIn: StateFlow<Boolean>
    val currentSession: StateFlow<UserSession?>

    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun register(email: String, password: String): Result<Unit>
    fun continueAsGuest(): UserSession
    suspend fun logout()
    fun checkAuthStatus()
    fun getCurrentSession(): UserSession?
}

@Singleton
class AuthServiceImpl @Inject constructor(private val secureStorageService: SecureStorageService) :
        AuthService {

    private val _currentSession = MutableStateFlow<UserSession?>(null)
    override val currentSession: StateFlow<UserSession?> = _currentSession.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    override val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    init {
        checkAuthStatus()
    }

    override fun checkAuthStatus() {
        val session = secureStorageService.getUserSession()
        _currentSession.value = session
        _isLoggedIn.value = session != null
    }

    override fun getCurrentSession(): UserSession? = _currentSession.value

    override fun continueAsGuest(): UserSession {
        val guestSession = UserSession(
            userId = "guest_${UUID.randomUUID().toString().take(8)}",
            email = null,
            displayName = "Guest Student",
            isGuest = true,
            token = null,
            createdAt = System.currentTimeMillis()
        )
        secureStorageService.saveUserSession(guestSession)
        _currentSession.value = guestSession
        _isLoggedIn.value = true
        return guestSession
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        delay(1000) // Simulate network call
        if (email == "admin" && password == "admin") {
            val mockToken = "mock_token_${System.currentTimeMillis()}"
            val session = UserSession(
                userId = "usr_${UUID.randomUUID().toString().take(8)}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                isGuest = false,
                token = mockToken,
                createdAt = System.currentTimeMillis()
            )
            secureStorageService.saveUserSession(session)
            _currentSession.value = session
            _isLoggedIn.value = true
            return Result.success(Unit)
        }
        return Result.failure(Exception("Invalid username or password."))
    }

    override suspend fun register(email: String, password: String): Result<Unit> {
        delay(1000) // Simulate network call
        if (email == "admin" && password == "admin") {
            val mockToken = "mock_token_${System.currentTimeMillis()}"
            val session = UserSession(
                userId = "usr_${UUID.randomUUID().toString().take(8)}",
                email = email,
                displayName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                isGuest = false,
                token = mockToken,
                createdAt = System.currentTimeMillis()
            )
            secureStorageService.saveUserSession(session)
            _currentSession.value = session
            _isLoggedIn.value = true
            return Result.success(Unit)
        }
        return Result.failure(
                Exception("Registration failed. Please try again.")
        )
    }

    override suspend fun logout() {
        delay(500)
        secureStorageService.clearUserSession()
        _currentSession.value = null
        _isLoggedIn.value = false
    }
}
