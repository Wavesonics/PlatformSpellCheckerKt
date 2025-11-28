package com.darkrockstudios.libs.platformspellchecker

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isCompactHeight = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            App(isCompactHeight = isCompactHeight)
        }
    }
}
