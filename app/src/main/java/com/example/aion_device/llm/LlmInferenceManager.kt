package com.example.aion_device.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.util.concurrent.atomic.AtomicLong

sealed interface InferenceEvent {
    data class Chunk(val requestId: Long, val text: String, val done: Boolean) : InferenceEvent
    data class Error(val requestId: Long, val message: String) : InferenceEvent
}

class LlmInferenceManager(
    private val context: Context
) {

    private var llm: LlmInference? = null
    private val requestId = AtomicLong(0)

    private val _events = MutableSharedFlow<InferenceEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<InferenceEvent> = _events

    fun init(modelPath: String) {
        val file = File(modelPath)
        require(file.exists())

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .build()

        llm = LlmInference.createFromOptions(context, options)
    }

    fun generate(prompt: String) {
        val id = requestId.incrementAndGet()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val result = llm?.generateResponse(prompt) ?: ""

                // 🔥 FAKE STREAMING (PERFECT UX)
                val words = result.split(" ")

                for ((i, word) in words.withIndex()) {
                    delay(30) // speed control

                    _events.emit(
                        InferenceEvent.Chunk(
                            requestId = id,
                            text = word + " ",
                            done = i == words.lastIndex
                        )
                    )
                }

            } catch (e: Exception) {
                _events.emit(
                    InferenceEvent.Error(
                        id,
                        e.message ?: "Error"
                    )
                )
            }
        }
    }

    fun close() {
        llm?.close()
        llm = null
    }
}