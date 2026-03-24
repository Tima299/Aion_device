package com.example.aion_device.llm

import android.content.Context
import com.example.aion_device.model.InferenceConfig
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface InferenceEvent {
    data class Chunk(val requestId: Long, val text: String, val done: Boolean) : InferenceEvent
    data class Failure(val requestId: Long, val message: String) : InferenceEvent
}

class LlmInferenceManager(
    private val context: Context,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var llm: LlmInference? = null
    private var initializedModelPath: String? = null

    private val _events = MutableSharedFlow<InferenceEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<InferenceEvent> = _events

    @Synchronized
    fun ensureInitialized(modelPath: String, config: InferenceConfig) {
        val file = File(modelPath)
        require(file.exists()) { "Model file does not exist: $modelPath" }

        if (llm != null && initializedModelPath == file.absolutePath) return

        llm?.close()

        val optionsBuilder = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .also { applyIntegerOptionCompat(it, config.maxTokens, "setMaxTokens") }
            .also { applyTopKCompat(it, config.topK) }
            .also { applyFloatOptionCompat(it, config.temperature, "setTemperature") }
            .also { applyIntegerOptionCompat(it, config.randomSeed, "setRandomSeed") }

        val options = optionsBuilder.build()

        llm = LlmInference.createFromOptions(context, options)
        initializedModelPath = file.absolutePath
    }



    private fun applyIntegerOptionCompat(
        builder: LlmInference.LlmInferenceOptions.Builder,
        value: Int,
        vararg methodNames: String,
    ) {
        applyOptionCompat(builder, value, Int::class.javaPrimitiveType, *methodNames)
    }

    private fun applyFloatOptionCompat(
        builder: LlmInference.LlmInferenceOptions.Builder,
        value: Float,
        vararg methodNames: String,
    ) {
        applyOptionCompat(builder, value, Float::class.javaPrimitiveType, *methodNames)
    }

    private fun applyOptionCompat(
        builder: LlmInference.LlmInferenceOptions.Builder,
        value: Any,
        parameterType: Class<*>?,
        vararg methodNames: String,
    ) {
        val method = builder.javaClass.methods.firstOrNull { candidate ->
            candidate.name in methodNames &&
                    candidate.parameterTypes.contentEquals(arrayOf(parameterType))
        } ?: return

        method.invoke(builder, value)
    }

    private fun applyTopKCompat(builder: LlmInference.LlmInferenceOptions.Builder, topK: Int) {
        applyIntegerOptionCompat(builder, topK, "setMaxTopK", "setTopK")
    }

    fun estimateTokens(prompt: String): Int {
        return prompt.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun generateResponseAsync(requestId: Long, prompt: String) {
        scope.launch {
            try {
                val activeLlm = llm ?: error("Model is not initialized")
                val result = activeLlm.generateResponse(prompt)
                emitChunkedResult(requestId, result)
            } catch (t: Throwable) {
                _events.emit(
                    InferenceEvent.Failure(
                        requestId = requestId,
                        message = t.message ?: "Unknown generation error",
                    ),
                )
            }
        }
    }

    private suspend fun emitChunkedResult(requestId: Long, fullText: String) {
        if (fullText.isBlank()) {
            _events.emit(InferenceEvent.Chunk(requestId = requestId, text = "", done = true))
            return
        }

        val words = fullText.split(" ")
        for ((index, word) in words.withIndex()) {
            delay(20)
            _events.emit(
                InferenceEvent.Chunk(
                    requestId = requestId,
                    text = if (index == words.lastIndex) word else "$word ",
                    done = index == words.lastIndex,
                ),
            )
        }
    }

    @Synchronized
    fun close() {
        llm?.close()
        llm = null
        initializedModelPath = null
    }
}
