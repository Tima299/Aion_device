package com.example.aion_device.llm

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.example.aion_device.model.InferenceConfig
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File

sealed interface InferenceEvent {
    data class Chunk(val requestId: Long, val text: String, val done: Boolean) : InferenceEvent
    data class Failure(val requestId: Long?, val message: String) : InferenceEvent
}

class LlmInferenceManager(
    private val context: Context,
    private val workerDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private var llm: LlmInference? = null
    private var loadedModelPath: String? = null
    private val managerScope = CoroutineScope(SupervisorJob() + workerDispatcher)
    private var activeGenerationJob: Job? = null

    private val _events = MutableSharedFlow<InferenceEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<InferenceEvent> = _events.asSharedFlow()

    fun ensureInitialized(modelPath: String, config: InferenceConfig) {
        if (llm != null && loadedModelPath == modelPath) return

        close()

        val file = File(modelPath)
        validateModelFile(file)

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .build()

        llm = LlmInference.createFromOptions(context, options)
        loadedModelPath = modelPath
    }

    fun estimateTokens(prompt: String): Int {
        return prompt.trim().split(Regex("\\s+")).count { it.isNotBlank() }
    }

    fun generateResponseAsync(requestId: Long, prompt: String) {
        val currentLlm = llm
        if (currentLlm == null) {
            managerScope.launch {
                _events.emit(
                    InferenceEvent.Failure(
                        requestId = requestId,
                        message = "Inference engine not initialized.",
                    ),
                )
            }
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = managerScope.launch {
            runCatching {
                currentLlm.generateResponse(prompt)
            }.onSuccess { response ->
                val chunks = response
                    .split(Regex("(?<=\\s)"))
                    .filter { it.isNotEmpty() }
                    .chunked(6)
                    .map { it.joinToString(separator = "") }

                if (chunks.isEmpty()) {
                    _events.emit(
                        InferenceEvent.Chunk(
                            requestId = requestId,
                            text = "",
                            done = true,
                        ),
                    )
                } else {
                    chunks.forEachIndexed { index, chunk ->
                        _events.emit(
                            InferenceEvent.Chunk(
                                requestId = requestId,
                                text = chunk,
                                done = index == chunks.lastIndex,
                            ),
                        )
                    }
                }
            }.onFailure { throwable ->
                _events.emit(
                    InferenceEvent.Failure(
                        requestId = requestId,
                        message = throwable.message ?: "Generation failed",
                    ),
                )
            }
        }
    }

    fun cancelActiveRequest() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
    }

    private fun validateModelFile(file: File) {
        require(file.exists() && file.isFile) {
            "Model file does not exist: ${file.absolutePath}"
        }
        require(file.length() > 50L * 1024L * 1024L) {
            "Model file looks too small or incomplete."
        }
        val extension = file.extension.lowercase()
        require(extension == "task" || extension == "bin") {
            "Unsupported model extension: .$extension"
        }
    }

    fun close() {
        cancelActiveRequest()
        llm?.close()
        llm = null
        loadedModelPath = null
    }

    fun dispose() {
        close()
        managerScope.cancel()
    }
}
