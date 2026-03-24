package com.example.aion_device.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.aion_device.ui.components.SectionCard
import com.example.aion_device.viewmodel.OnDeviceUiState

@Composable
fun StatusScreen(
    state: OnDeviceUiState,
    contentPadding: PaddingValues,
    onRefreshModel: () -> Unit,
    onImportModel: () -> Unit,
) {
    val adbCommand = "adb push model_version.task /sdcard/Android/data/com.example.aion_device/files/models/model_version.task"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(
            title = "Model readiness",
            subtitle = "Current local AI engine state",
        ) {
            ListItem(
                headlineContent = { Text(if (state.modelInfo.isInstalled) "Installed" else "Not installed") },
                supportingContent = { Text(state.modelInfo.guidance) },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                    )
                },
            )

            if (state.modelInfo.isInstalled) {
                HorizontalDivider()
                Text("File: ${state.modelInfo.fileName}")
                Text("Size: ${state.modelInfo.sizeInMb}")
                Text("Path: ${state.modelInfo.absolutePath}")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onRefreshModel,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("Rescan", modifier = Modifier.padding(start = 8.dp))
            }

            Button(
                onClick = onImportModel,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Text("Import model", modifier = Modifier.padding(start = 8.dp))
            }
        }

        SectionCard(
            title = "Graphics processing",
            subtitle = "UI and inference pipeline overview",
        ) {
            Text("Compose UI: Android hardware-accelerated rendering")
            Text("LLM runtime: MediaPipe LLM Inference engine")
            Text("Recommended model format today: .task bundle")
            Text("Best testing target: real Android phone, not emulator")
        }

        SectionCard(
            title = "Runtime metrics",
            subtitle = "Latest measured values from this session",
        ) {
            Text("Prompt tokens: ${state.stats.lastPromptTokens}")
            Text("Response characters: ${state.stats.lastResponseCharacters}")
            Text("Last latency: ${state.stats.lastLatencyMs ?: 0} ms")
        }

        SectionCard(
            title = "ADB setup",
            subtitle = "Fastest manual install path during development",
        ) {
            Text(
                text = adbCommand,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            text = state.latestInfo ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
