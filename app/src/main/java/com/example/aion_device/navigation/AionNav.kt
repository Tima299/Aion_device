package com.example.aion_device.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aion_device.ui.AionApp
import com.example.aion_device.viewmodel.OnDeviceViewModel

@Composable
fun AionNav() {
    val viewModel: OnDeviceViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()

    AionApp(
        state = state,
        onInputChanged = viewModel::onInputChanged,
        onSendClicked = viewModel::sendPrompt,
        onUseQuickPrompt = viewModel::useQuickPrompt,
        onRefreshModel = viewModel::refreshModelState,
        onImportModel = viewModel::importModelFromUri,
        onClearChat = viewModel::clearChat,
        onDismissError = viewModel::dismissError,
    )
}
