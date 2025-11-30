package com.darkrockstudios.libs.platformspellchecker

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier
import platform.Foundation.preferredLanguages
import platform.UIKit.UITextChecker

actual class PlatformSpellCheckerFactory() {
	actual suspend fun createSpellChecker(locale: SpLocale?): PlatformSpellChecker {
		return if (locale == null) {
			PlatformSpellChecker()
		} else {
			if (!hasLanguage(locale)) {
				throw IllegalArgumentException("Locale not supported: ${locale.language}${locale.country?.let { "_$it" } ?: ""}")
			}
			PlatformSpellChecker(locale)
		}
	}

	actual fun hasLanguage(locale: SpLocale): Boolean {
		if (locale.language.isBlank()) return false
		val normalized = if (locale.country.isNullOrBlank()) locale.language else "${locale.language}_${locale.country}"
		val available = UITextChecker.availableLanguages() as? List<*>
		return available?.any { it as? String == normalized } == true
	}

	actual fun isAvailable(): Boolean {
		val langs = UITextChecker.availableLanguages() as? List<*>
		return langs?.isNotEmpty() == true
	}

	actual fun currentSystemLocale(): SpLocale {
		val preferred = (NSLocale.preferredLanguages.firstOrNull() as? String)
		val tag = preferred ?: NSLocale.currentLocale.localeIdentifier
		val us = tag.replace('-', '_')
		val parts = us.split('_')
		val lang = parts.getOrNull(0)?.lowercase() ?: ""
		val country = parts.getOrNull(1)?.uppercase()
		return SpLocale(lang, country)
	}

	actual fun availableLocales(): List<SpLocale> {
		val langs = (UITextChecker.availableLanguages() as? List<*>)?.mapNotNull { it as? String } ?: return emptyList()
		return langs.map { s ->
			val parts = s.split('_')
			val lang = parts.getOrNull(0)?.lowercase() ?: ""
			val country = parts.getOrNull(1)?.uppercase()
			SpLocale(lang, country)
		}
	}
}
