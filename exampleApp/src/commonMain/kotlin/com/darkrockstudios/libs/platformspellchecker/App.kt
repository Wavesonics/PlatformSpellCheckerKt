package com.darkrockstudios.libs.platformspellchecker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.darkrockstudios.libs.platformspellchecker.ui.SpellCheckContent
import com.darkrockstudios.libs.platformspellchecker.ui.theme.PlatformSpellCheckerTheme
import org.koin.compose.KoinContext

@Composable
fun App(
    isCompactHeight: Boolean = false
) {
    KoinContext {
        PlatformSpellCheckerTheme {
            Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                SpellCheckContent(
                    isCompactHeight = isCompactHeight,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
