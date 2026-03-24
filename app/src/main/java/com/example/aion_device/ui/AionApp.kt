package com.example.aion_device.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.aion_device.model.AppTab
import com.example.aion_device.ui.components.AppTopBar
import com.example.aion_device.ui.screens.ChatScreen
import com.example.aion_device.ui.screens.ProfileScreen
import com.example.aion_device.ui.screens.StatusScreen
import com.example.aion_device.viewmodel.OnDeviceUiState

@Composable
fun AionApp(
    state: OnDeviceUiState,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onUseQuickPrompt: (String) -> Unit,
    onRefreshModel: () -> Unit,
    onImportModel: (Uri) -> Unit,
    onClearChat: () -> Unit,
    onDismissError: () -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(AppTab.CHAT) }
    val snackbarHostState = remember { SnackbarHostState() }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onImportModel(uri)
    }

    LaunchedEffect(state.latestError) {
        val error = state.latestError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        onDismissError()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = currentTab.title,
                isModelReady = state.modelInfo.isInstalled,
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            if (tab == AppTab.STATUS && !state.modelInfo.isInstalled) {
                                Badge { Text("!") }
                            }
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                            )
                        },
                        label = { Text(tab.title) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when (currentTab) {
            AppTab.CHAT -> ChatScreen(
                state = state,
                contentPadding = padding,
                onInputChanged = onInputChanged,
                onSendClicked = onSendClicked,
                onUseQuickPrompt = onUseQuickPrompt,
                onClearChat = onClearChat,
            )

            AppTab.STATUS -> StatusScreen(
                state = state,
                contentPadding = padding,
                onRefreshModel = onRefreshModel,
                onImportModel = {
                    importLauncher.launch(arrayOf("*/*"))
                },
            )

            AppTab.PROFILE -> ProfileScreen(
                state = state,
                contentPadding = padding,
            )
        }
    }
}
