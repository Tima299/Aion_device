package com.example.aion_device.model

data class InferenceStats(
    val lastPromptTokens: Int = 0,
    val lastResponseCharacters: Int = 0,
    val lastLatencyMs: Long? = null,
)
