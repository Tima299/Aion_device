package com.example.aion_device.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.example.aion_device.model.InferenceConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

sealed interface InferenceEvent {
    data class Chunk(val requestId: Long, val text: String, val done: Boolean) : InferenceEvent
    data class Failure(val requestId: Long, val message: String) : InferenceEvent
}

class LlmInferenceManager(
    private val context: Context
) {

    private var llm: LlmInference? = null
    private var initializedModelPath: String? = null
    private val requestId = AtomicLong(0)
    private val activeAsyncRequestId = AtomicLong(0)
    private val streamedTextByRequest = ConcurrentHashMap<Long, String>()

    private val _events = MutableSharedFlow<InferenceEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<InferenceEvent> = _events

    private fun init(modelPath: String, config: InferenceConfig) {
        val file = File(modelPath)
        require(file.exists()) { "Model file not found at $modelPath" }

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .setResultListener { partialResult, done ->
                val currentRequest = activeAsyncRequestId.get()
                if (currentRequest == 0L) return@setResultListener

                val currentText = partialResult.toString()
                val previous = streamedTextByRequest[currentRequest].orEmpty()
                val delta = if (currentText.startsWith(previous)) {
                    currentText.removePrefix(previous)
                } else {
                    currentText
                }

                streamedTextByRequest[currentRequest] = currentText

                CoroutineScope(Dispatchers.Default).launch {
                    _events.emit(
                        InferenceEvent.Chunk(
                            requestId = currentRequest,
                            text = delta,
                            done = done,
                        )
                    )
                    if (done) {
                        streamedTextByRequest.remove(currentRequest)
                    }
                }
            }
            .build()

        llm = LlmInference.createFromOptions(context, options)
        initializedModelPath = modelPath
    }

    @Synchronized
    fun ensureInitialized(modelPath: String, config: InferenceConfig) {
        if (llm != null && initializedModelPath == modelPath) return
        close()
        init(modelPath, config)
    }

    fun estimateTokens(prompt: String): Int {
        if (prompt.isBlank()) return 0
        return prompt.trim().split(Regex("\\s+")).size
    }

    fun generateResponseAsync(requestId: Long? = null, prompt: String) {
        val id = requestId ?: this.requestId.incrementAndGet()
        activeAsyncRequestId.set(id)
        streamedTextByRequest.remove(id)

        runCatching {
            llm?.generateResponseAsync(prompt) ?: error("LLM is not initialized.")
        }.onFailure { e ->
            CoroutineScope(Dispatchers.Default).launch {
                _events.emit(
                    InferenceEvent.Failure(
                        requestId = id,
                        message = e.message ?: "Error",
                    )
                )
            }
        }
    }

    fun close() {
        llm?.close()
        llm = null
        initializedModelPath = null
        activeAsyncRequestId.set(0)
        streamedTextByRequest.clear()
    }
}