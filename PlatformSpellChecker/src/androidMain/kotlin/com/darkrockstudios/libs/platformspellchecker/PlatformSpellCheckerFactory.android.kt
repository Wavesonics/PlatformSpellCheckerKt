package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import android.os.Build
import android.os.LocaleList

actual class PlatformSpellCheckerFactory(private val context: Context) {

	actual suspend fun createSpellChecker(locale: SpLocale?): PlatformSpellChecker {
		return if (locale == null) {
			PlatformSpellChecker(context)
		} else {
			if (!hasLanguage(locale)) {
				throw IllegalArgumentException("Locale not supported: ${locale.language}${locale.country?.let { "-$it" } ?: ""}")
			}
			PlatformSpellChecker(context, locale)
		}
	}

	actual fun hasLanguage(locale: SpLocale): Boolean {
		if (locale.language.isBlank()) return false
		val requestedLang = locale.language.lowercase()

		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			val list: LocaleList = context.resources.configuration.locales
			(0 until list.size()).any { index ->
				val l = list[index]
				val sysLang = l.language?.lowercase().orEmpty()
				sysLang.isNotBlank() && sysLang == requestedLang
			}
		} else {
			val lc = context.resources.configuration.locale
			val sysLang = lc?.language?.lowercase().orEmpty()
			sysLang.isNotBlank() && sysLang == requestedLang
		}
	}

	actual fun isAvailable(): Boolean {
		return context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) != null
	}

	actual fun currentSystemLocale(): SpLocale {
		val jl = java.util.Locale.getDefault()
		val lang = jl.language.ifBlank { "" }
		val country = jl.country.ifBlank { null }
		return SpLocale(lang, country)
	}

	actual fun availableLocales(): List<SpLocale> {
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			val list = LocaleList.getDefault()
			(0 until list.size()).map { idx ->
				val l = list[idx]
				val lang = l.language.ifBlank { "" }
				val country = l.country.ifBlank { null }
				SpLocale(lang, country)
			}.distinct()
		} else {
			java.util.Locale.getAvailableLocales().mapNotNull { l ->
				val lang = l.language
				if (lang.isNullOrBlank()) null else SpLocale(lang, l.country.ifBlank { null })
			}.distinctBy { it.language + (it.country ?: "") }
		}
	}
}
