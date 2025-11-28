package com.darkrockstudios.libs.platformspellchecker

import androidx.compose.ui.window.ComposeUIViewController
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import platform.UIKit.UIViewController

/**
 * iOS entry point for the Compose Multiplatform app.
 * This function is called from the iOS side to create the main UIViewController.
 */
fun MainViewController(): UIViewController {
    // Initialize Koin with iOS module
    startKoin {
        modules(iosModule)
    }

    return ComposeUIViewController {
        App(isCompactHeight = false)
    }
}

/**
 * Koin module for iOS with PlatformSpellChecker and ViewModel setup
 */
private val iosModule = module {
    single { PlatformSpellChecker() }
    viewModel { SpellCheckViewModel(get()) }
}
