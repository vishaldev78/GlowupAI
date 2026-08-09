package com.glowup.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val avatar: String? = null,
    val provider: String = "email"
)