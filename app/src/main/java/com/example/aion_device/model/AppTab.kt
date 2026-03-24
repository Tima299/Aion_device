package com.example.aion_device.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
) {
    CHAT("chat", "Local Chat", Icons.Default.Psychology),
    STATUS("status", "Model Status", Icons.Default.Memory),
    PROFILE("profile", "Profile", Icons.Default.Person),
}
