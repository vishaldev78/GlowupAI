package com.glowup.ai.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TryOnRequest(
    val personImageBase64: String,
    val garmentImageBase64: String,
    val garmentCategory: String
)

@Serializable
data class TryOnResponse(
    val success: Boolean = false,
    val resultImageUrl: String? = null,
    val error: String? = null,
    val code: String? = null
)