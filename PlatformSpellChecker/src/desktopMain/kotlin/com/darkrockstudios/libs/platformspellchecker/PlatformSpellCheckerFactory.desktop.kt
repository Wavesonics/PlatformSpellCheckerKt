package com.darkrockstudios.libs.platformspellchecker

actual class PlatformSpellCheckerFactory() {
    actual suspend fun createSpellChecker(locale: SpLocale?): PlatformSpellChecker {
        return if (locale == null) {
            PlatformSpellChecker()
        } else {
            if (!hasLanguage(locale)) {
                throw IllegalArgumentException("Locale not supported: ${locale.language}${locale.country?.let { "-$it" } ?: ""}")
            }
            PlatformSpellChecker(locale)
        }
    }

    actual fun hasLanguage(locale: SpLocale): Boolean {
        if (locale.language.isBlank()) return false
        val tag = if (locale.country.isNullOrBlank()) locale.language else "${locale.language}-${locale.country}"
        return try {
            com.darkrockstudios.libs.platformspellchecker.native.NativeSpellCheckerFactory.isLanguageSupported(tag)
        } catch (_: Throwable) { false }
    }

    actual fun isAvailable(): Boolean = try {
        com.darkrockstudios.libs.platformspellchecker.native.NativeSpellCheckerFactory.isAvailable()
    } catch (_: Throwable) { false }

    actual fun currentSystemLocale(): SpLocale {
        return try {
            val jl = java.util.Locale.getDefault()
            val lang = jl.language.ifBlank { "" }
            val country = jl.country.ifBlank { null }
            SpLocale(lang, country)
        } catch (_: Throwable) {
            SpLocale("")
        }
    }

    actual fun availableLocales(): List<SpLocale> {
        // Best-effort; native backends may not expose enumeration. Return current as a minimum.
        return listOf(currentSystemLocale())
    }
}
