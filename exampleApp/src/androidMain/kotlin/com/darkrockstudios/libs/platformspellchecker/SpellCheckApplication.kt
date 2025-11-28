package com.darkrockstudios.libs.platformspellchecker

import android.app.Application
import com.darkrockstudios.libs.platformspellchecker.PlatformSpellChecker
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

class SpellCheckApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@SpellCheckApplication)
            modules(appModule)
        }
    }

    private val appModule = module {
        single { PlatformSpellChecker(androidContext()) }
        viewModel { SpellCheckViewModel(get()) }
    }
}
