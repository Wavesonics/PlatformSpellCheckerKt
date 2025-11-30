package com.darkrockstudios.libs.platformspellchecker

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun main() = application {
	startKoin {
		modules(desktopModule)
	}

	Window(
		onCloseRequest = ::exitApplication,
		title = "PlatformSpellChecker",
		state = rememberWindowState(width = 800.dp, height = 600.dp)
	) {
		App(isCompactHeight = false)
	}
}

private val desktopModule = module {
	single { PlatformSpellChecker() }
	viewModel { SpellCheckViewModel(get()) }
}
