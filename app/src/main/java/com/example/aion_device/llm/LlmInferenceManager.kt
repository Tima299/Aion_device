package com.example.aion_device.llm

import android.content.Context
import com.example.aion_device.model.InferenceConfig
import com.google.common.util.concurrent.MoreExecutors
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.max

sealed interface InferenceEvent {
    data class Chunk(val requestId: Long, val text: String, val done: Boolean) : InferenceEvent
    data class Failure(val requestId: Long, val message: String) : InferenceEvent
}

class LlmInferenceManager(
    private val context: Context,
) {
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var llm: LlmInference? = null
    private var session: LlmInferenceSession? = null
    private var initializedModelPath: String? = null
    private var activeGeneration: Future<String>? = null

    private val _events = MutableSharedFlow<InferenceEvent>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val events: SharedFlow<InferenceEvent> = _events

    private val sequence = AtomicLong(0)

    private val modelsDir: File by lazy {
        File(context.filesDir, "models").apply { mkdirs() }
    }

    fun localModelPathFor(modelUrl: String): String {
        val fileName = URL(modelUrl).path.substringAfterLast('/').ifBlank { "model.task" }
        return File(modelsDir, fileName).absolutePath
    }

    fun isModelDownloaded(modelUrl: String): Boolean = File(localModelPathFor(modelUrl)).exists()

    suspend fun downloadModel(
        modelUrl: String,
        huggingFaceToken: String? = null,
        onProgress: (downloadedBytes: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): File {
        val target = File(localModelPathFor(modelUrl))
        if (target.exists() && target.length() > 0L) return target

        val tempFile = File(target.absolutePath + ".partial")
        val connection = (URL(modelUrl).openConnection() as HttpURLConnection).apply {
            connectTimeout = 30_000
            readTimeout = 30_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/octet-stream")
            if (!huggingFaceToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $huggingFaceToken")
            }
        }

        try {
            connection.connect()
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Download failed with HTTP ${connection.responseCode}")
            }

            val total = connection.contentLengthLong
            var downloaded = 0L

            connection.inputStream.use { input ->
                tempFile.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        onProgress(downloaded, total)
                    }
                }
            }

            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
                tempFile.delete()
            }

            return target
        } finally {
            connection.disconnect()
        }
    }

    fun ensureInitialized(modelPath: String, config: InferenceConfig) {
        if (initializedModelPath == modelPath && llm != null && session != null) return

        val file = File(modelPath)
        require(file.exists()) { "Model file not found at $modelPath" }

        close()

        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(file.absolutePath)
            .setMaxTokens(config.maxTokens)
            .build()

        llm = LlmInference.createFromOptions(context, options)

        val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTopK(config.topK)
            .setTemperature(config.temperature)
            .build()

        session = LlmInferenceSession.createFromOptions(checkNotNull(llm), sessionOptions)
        initializedModelPath = modelPath
    }

    fun estimateTokens(prompt: String): Int {
        val activeSession = session ?: return -1
        return max(0, activeSession.sizeInTokens(prompt))
    }

    fun generateResponseAsync(requestId: Long? = null, prompt: String) {
        val id = requestId ?: sequence.incrementAndGet()
        val activeSession = session
        if (activeSession == null) {
            managerScope.launch {
                _events.emit(InferenceEvent.Failure(id, "No model initialized"))
            }
            return
        }

        activeSession.addQueryChunk(prompt)
        val future = activeSession.generateResponseAsync { partialText, done ->
            managerScope.launch {
                _events.emit(InferenceEvent.Chunk(id, partialText, done))
            }
        }
        activeGeneration = future

        Futures.addCallback(
            future,
            object : FutureCallback<String> {
                override fun onSuccess(result: String?) = Unit

                override fun onFailure(t: Throwable) {
                    managerScope.launch {
                        _events.emit(InferenceEvent.Failure(id, t.message ?: "Generation failed"))
                    }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun cancelActiveRequest() {
        activeGeneration?.cancel(true)
        activeGeneration = null
    }

    fun dispose() {
        close()
    }

    fun close() {
        cancelActiveRequest()
        runCatching { session?.close() }
        runCatching { llm?.close() }
        session = null
        llm = null
        initializedModelPath = null
    }
}