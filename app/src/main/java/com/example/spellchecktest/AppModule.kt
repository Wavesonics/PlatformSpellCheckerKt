package com.example.spellchecktest

import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Repository
    single<SpellCheckRepository> { SpellCheckRepositoryImpl(androidContext()) }

    // ViewModel
    viewModel { SpellCheckViewModel(get()) }
}
