package com.example.aion_device.model

enum class ChatRole {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: Long,
    val role: ChatRole,
    val text: String,
    val isStreaming: Boolean = false,
)
