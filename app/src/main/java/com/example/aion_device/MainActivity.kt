package com.example.aion_device

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.aion_device.navigation.AionNav
import com.example.aion_device.ui.theme.AionDeviceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AionDeviceTheme {
                AionNav()
            }
        }
    }
}
