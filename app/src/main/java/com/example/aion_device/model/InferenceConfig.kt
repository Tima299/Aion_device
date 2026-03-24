package com.example.aion_device.model

data class InferenceConfig(
    val maxTokens: Int = 512,
    val topK: Int = 40,
    val temperature: Float = 0.7f,
    val randomSeed: Int = 7,
)
