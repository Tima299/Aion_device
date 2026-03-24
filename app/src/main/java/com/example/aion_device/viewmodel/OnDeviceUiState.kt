package com.example.aion_device.viewmodel

import com.example.aion_device.model.ChatMessage
import com.example.aion_device.model.ChatRole
import com.example.aion_device.model.InferenceStats
import com.example.aion_device.model.ModelInfo

data class OnDeviceUiState(
    val input: String = "",
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = 1L,
            role = ChatRole.ASSISTANT,
            text = "Welcome to AION Device. Import a local model on the Status tab, then start chatting privately on your phone.",
        ),
    ),
    val isGenerating: Boolean = false,
    val activeRequestId: Long? = null,
    val modelInfo: ModelInfo = ModelInfo(),
    val latestError: String? = null,
    val latestInfo: String? = "Ready for local setup.",
    val stats: InferenceStats = InferenceStats(),
)
