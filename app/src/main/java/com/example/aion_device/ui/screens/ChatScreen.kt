package com.example.aion_device.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aion_device.ui.components.AvatarAchievementHeader
import com.example.aion_device.ui.components.ChatBubble
import com.example.aion_device.viewmodel.OnDeviceUiState

@Composable
fun ChatScreen(
    state: OnDeviceUiState,
    contentPadding: PaddingValues,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onUseQuickPrompt: (String) -> Unit,
    onClearChat: () -> Unit,
) {
    val listState = rememberLazyListState()
    val quickPrompts = remember {
        listOf(
            "Explain this Android code clearly",
            "Summarize my plan in bullet points",
            "Rewrite this into a professional email",
            "Give me a step-by-step debugging checklist",
        )
    }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarAchievementHeader(
            title = "AION Device",
            subtitle = "Private on-device AI assistant",
            achievementLabel = "Avatar slot ready for my_photo.png",
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickPrompts.take(2).forEach { prompt ->
                AssistChip(
                    onClick = { onUseQuickPrompt(prompt) },
                    label = { Text(prompt) },
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            quickPrompts.drop(2).forEach { prompt ->
                AssistChip(
                    onClick = { onUseQuickPrompt(prompt) },
                    label = { Text(prompt) },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(
                items = state.messages,
                key = { it.id },
            ) { message ->
                ChatBubble(message = message)
            }
        }

        Text(
            text = state.latestInfo ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.input,
            onValueChange = onInputChanged,
            placeholder = { Text("Ask locally on your phone…") },
            enabled = !state.isGenerating,
            minLines = 2,
            maxLines = 6,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ElevatedButton(
                onClick = onClearChat,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.elevatedButtonColors(),
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "Clear chat",
                )
                Text(
                    text = "Clear",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            FilledIconButton(
                onClick = onSendClicked,
                modifier = Modifier.size(56.dp),
                enabled = !state.isGenerating,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                )
            }
        }
    }
}
