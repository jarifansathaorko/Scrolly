package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.tracking.detector.NotchDetectionHelper
import com.example.ui.navigation.ScrollyAppScaffold
import com.example.ui.theme.ScrollyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Detect device physical cutout and calibrate notch placement
        window.decorView.post {
            try {
                val detected = NotchDetectionHelper.detectFromWindow(window, this)
                val repo = ScrollyApp.instance.notchSettingsRepository
                // If not manually calibrated yet, automatically adopt the detected hardware cutout
                if (!repo.configFlow.value.autoAdjusted || detected.hasCutout) {
                    repo.autoCalibrateWithCutout(detected)
                }
            } catch (_: Exception) {}
        }

        setContent {
            ScrollyTheme {
                ScrollyAppScaffold()
            }
        }
    }
}
