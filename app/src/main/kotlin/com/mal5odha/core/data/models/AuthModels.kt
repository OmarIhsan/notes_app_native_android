package com.mal5odha.core.data.models

/**
 * Model representing the active user session state, distinguishing between
 * registered/authenticated users and local ephemeral guest sessions.
 */
data class UserSession(
    val userId: String,
    val email: String?,
    val displayName: String,
    val isGuest: Boolean,
    val token: String?,
    val createdAt: Long
)
