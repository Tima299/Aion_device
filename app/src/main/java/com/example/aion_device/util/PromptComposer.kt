package com.example.aion_device.util

import com.example.aion_device.model.ChatMessage
import com.example.aion_device.model.ChatRole

object PromptComposer {

    private const val SYSTEM_PROMPT = """
You are AION, a private on-device assistant running fully offline on Android.
Be concise, helpful, and practical.
Prefer structured answers for technical questions.
"""

    fun composePrompt(history: List<ChatMessage>, latestUserInput: String): String {
        val relevantMessages = history
            .filter { it.role != ChatRole.SYSTEM }
            .takeLast(8)

        val transcript = buildString {
            appendLine(SYSTEM_PROMPT.trim())
            appendLine()
            relevantMessages.forEach { message ->
                when (message.role) {
                    ChatRole.USER -> appendLine("User: ${message.text}")
                    ChatRole.ASSISTANT -> appendLine("Assistant: ${message.text}")
                    ChatRole.SYSTEM -> Unit
                }
            }
            appendLine("User: $latestUserInput")
            append("Assistant:")
        }

        return transcript
    }
}
