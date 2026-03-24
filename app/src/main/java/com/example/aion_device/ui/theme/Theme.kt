package com.example.aion_device.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary = CyberBlue,
    onPrimary = MidnightBlue,
    primaryContainer = DeepNavy,
    onPrimaryContainer = IceBlue,
    secondary = Emerald,
    background = MidnightBlue,
    surface = Color(0xFF111B31),
    surfaceVariant = Color(0xFF182746),
    onSurface = IceBlue,
    onSurfaceVariant = Color(0xFFB6C8E6),
    error = Color(0xFFFF6B6B),
)

@Composable
fun AionDeviceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current

    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicDarkColorScheme(context)
        }
        else -> DarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        typography = AionTypography,
        content = content,
    )
}
