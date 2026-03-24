package com.example.aion_device.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.aion_device.ui.components.AvatarAchievementHeader
import com.example.aion_device.ui.components.SectionCard
import com.example.aion_device.viewmodel.OnDeviceUiState

@Composable
fun ProfileScreen(
    state: OnDeviceUiState,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarAchievementHeader(
            title = "My Local AI Build",
            subtitle = "Put my_photo.png on the left and show the accomplishment clearly",
            achievementLabel = if (state.modelInfo.isInstalled) "Offline AI accomplished" else "UI accomplished • model pending",
        )

        SectionCard(
            title = "Accomplishment board",
            subtitle = "What this upgraded version now includes",
        ) {
            ListItem(
                headlineContent = { Text("Private on-device chat") },
                supportingContent = { Text("No cloud API required once the model is available locally.") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Better architecture") },
                supportingContent = { Text("Separated manager, locator, state, UI screens, and reusable components.") },
                leadingContent = { Icon(Icons.Default.DeveloperMode, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Model import workflow") },
                supportingContent = { Text("Pick a .task or .bin file directly from the phone instead of depending on flaky emulator downloads.") },
                leadingContent = { Icon(Icons.Default.Memory, contentDescription = null) },
            )
            ListItem(
                headlineContent = { Text("Achievement-ready profile UI") },
                supportingContent = { Text("Avatar placeholder plus verified badge to show the build is done.") },
                leadingContent = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
            )
        }

        SectionCard(
            title = "Avatar setup",
            subtitle = "How to show your own profile image",
        ) {
            Text("1. Add my_photo.png to app/src/main/res/drawable/")
            Text("2. Rebuild the app")
            Text("3. The profile and chat header will automatically use that image")
        }
    }
}
