package com.example.spellchecktest

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class SpellCheckApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            // Log Koin into Android logger
            androidLogger(Level.ERROR)
            // Reference Android context
            androidContext(this@SpellCheckApplication)
            // Load modules
            modules(appModule)
        }
    }
}
