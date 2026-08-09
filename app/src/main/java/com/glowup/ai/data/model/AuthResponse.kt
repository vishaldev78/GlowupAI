package com.glowup.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    val success: Boolean = false,
    val token: String = "",
    val user: User? = null,
    val error: String? = null
)

@Serializable
data class UserResponse(
    val user: User? = null
)

@Serializable
data class AuthRequest(
    val email: String,
    val password: String
)

@Serializable
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String
)
