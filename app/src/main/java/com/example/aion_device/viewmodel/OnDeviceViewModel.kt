package com.example.aion_device.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aion_device.llm.InferenceEvent
import com.example.aion_device.llm.LlmInferenceManager
import com.example.aion_device.llm.ModelFileLocator
import com.example.aion_device.model.ChatMessage
import com.example.aion_device.model.ChatRole
import com.example.aion_device.model.InferenceConfig
import com.example.aion_device.model.InferenceStats
import com.example.aion_device.model.ModelInfo
import com.example.aion_device.util.PromptComposer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class OnDeviceViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val inferenceManager = LlmInferenceManager(application)
    private val config = InferenceConfig()

    private val _uiState = MutableStateFlow(OnDeviceUiState())
    val uiState: StateFlow<OnDeviceUiState> = _uiState.asStateFlow()

    private var generationStartedAt: Long = 0L

    init {
        refreshModelState()
        observeInferenceEvents()
    }

    private fun observeInferenceEvents() {
        viewModelScope.launch {
            inferenceManager.events.collectLatest { event ->
                when (event) {
                    is InferenceEvent.Chunk -> handleChunk(event)
                    is InferenceEvent.Failure -> handleFailure(event)
                }
            }
        }
    }

    fun onInputChanged(value: String) {
        _uiState.update { it.copy(input = value) }
    }

    fun useQuickPrompt(text: String) {
        _uiState.update { it.copy(input = text) }
    }

    fun clearChat() {
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        id = System.currentTimeMillis(),
                        role = ChatRole.ASSISTANT,
                        text = "Chat cleared. Ready for the next local prompt.",
                    ),
                ),
                latestInfo = "Chat history cleared.",
                latestError = null,
            )
        }
    }

    fun cancelGeneration() {
        inferenceManager.cancelActiveRequest()
        _uiState.update {
            it.copy(
                isGenerating = false,
                activeRequestId = null,
                latestInfo = "Generation cancelled.",
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(latestError = null) }
    }

    fun refreshModelState() {
        val modelFile = ModelFileLocator.resolveBestModelFile(getApplication())
        if (modelFile == null) {
            inferenceManager.close()
            _uiState.update {
                it.copy(
                    modelInfo = ModelInfo(
                        isInstalled = false,
                        guidance = "No compatible model found. Import a .task model for the best current setup, or an older .bin model if that is what you already have.",
                    ),
                    latestInfo = "Model not installed yet.",
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                modelInfo = ModelInfo(
                    isInstalled = true,
                    fileName = modelFile.name,
                    absolutePath = modelFile.absolutePath,
                    sizeBytes = modelFile.length(),
                    guidance = "Model detected. Real on-device inference is available.",
                ),
                latestInfo = "Model detected: ${modelFile.name}",
                latestError = null,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                inferenceManager.close()
                inferenceManager.ensureInitialized(modelFile.absolutePath, config)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        latestError = "Initialization failed: ${throwable.message}",
                    )
                }
            }
        }
    }

    fun importModelFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    latestInfo = "Importing model…",
                    latestError = null,
                )
            }

            runCatching {
                val resolver = getApplication<Application>().contentResolver
                val fileName = queryDisplayName(uri) ?: "imported_model.task"
                val modelsDir = File(
                    getApplication<Application>().getExternalFilesDir(null),
                    "models",
                ).apply { mkdirs() }

                val targetFile = File(modelsDir, fileName)

                resolver.openInputStream(uri)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: error("Could not open selected file.")

                targetFile
            }.onSuccess { file ->
                _uiState.update {
                    it.copy(
                        latestInfo = "Imported ${file.name}. Scanning and initializing…",
                    )
                }
                refreshModelState()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        latestError = "Import failed: ${throwable.message}",
                    )
                }
            }
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        val resolver = getApplication<Application>().contentResolver
        resolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return null
    }

    fun sendPrompt() {
        val current = _uiState.value
        val userText = current.input.trim()

        if (userText.isBlank() || current.isGenerating) return

        val requestId = System.currentTimeMillis()
        generationStartedAt = System.currentTimeMillis()

        val userMessage = ChatMessage(
            id = requestId,
            role = ChatRole.USER,
            text = userText,
        )

        val placeholderAssistant = ChatMessage(
            id = requestId + 1,
            role = ChatRole.ASSISTANT,
            text = "",
            isStreaming = true,
        )

        _uiState.update {
            it.copy(
                input = "",
                isGenerating = true,
                activeRequestId = requestId,
                latestError = null,
                latestInfo = if (it.modelInfo.isInstalled) "Generating locally on device…" else "No model initialized.",
                messages = it.messages + userMessage + placeholderAssistant,
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val modelPath = _uiState.value.modelInfo.absolutePath
            if (modelPath.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        latestError = "No local model found. Import one on the Status tab first.",
                    )
                }
                return@launch
            }

            val prompt = PromptComposer.composePrompt(
                history = _uiState.value.messages.dropLast(1),
                latestUserInput = userText,
            )

            val tokenEstimate = inferenceManager.estimateTokens(prompt)

            _uiState.update {
                it.copy(
                    stats = it.stats.copy(lastPromptTokens = tokenEstimate),
                )
            }

            runCatching {
                inferenceManager.ensureInitialized(modelPath, config)
                inferenceManager.generateResponseAsync(requestId, prompt)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        latestError = "Could not start generation: ${throwable.message}",
                    )
                }
            }
        }
    }

    private fun handleChunk(event: InferenceEvent.Chunk) {
        val current = _uiState.value
        if (current.activeRequestId != event.requestId) return

        val updatedMessages = current.messages.toMutableList()
        val lastAssistantIndex = updatedMessages.indexOfLast {
            it.role == ChatRole.ASSISTANT && it.isStreaming
        }

        if (lastAssistantIndex >= 0) {
            val existing = updatedMessages[lastAssistantIndex]
            updatedMessages[lastAssistantIndex] = existing.copy(
                text = existing.text + event.text,
                isStreaming = !event.done,
            )
        }

        val lastAssistantText = updatedMessages.lastOrNull()?.text.orEmpty()
        val finishedLatency = if (event.done) {
            System.currentTimeMillis() - generationStartedAt
        } else {
            current.stats.lastLatencyMs
        }

        _uiState.update {
            it.copy(
                messages = updatedMessages,
                isGenerating = !event.done,
                activeRequestId = if (event.done) null else event.requestId,
                latestInfo = if (event.done) "Response completed." else "Streaming response…",
                stats = InferenceStats(
                    lastPromptTokens = it.stats.lastPromptTokens,
                    lastResponseCharacters = lastAssistantText.length,
                    lastLatencyMs = finishedLatency,
                ),
            )
        }
    }

    private fun handleFailure(event: InferenceEvent.Failure) {
        _uiState.update {
            it.copy(
                isGenerating = false,
                activeRequestId = null,
                latestError = event.message,
            )
        }
    }

    override fun onCleared() {
        inferenceManager.dispose()
        super.onCleared()
    }
}
