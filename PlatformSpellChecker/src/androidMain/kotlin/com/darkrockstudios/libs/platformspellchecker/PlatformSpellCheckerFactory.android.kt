package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import android.os.Build
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.view.textservice.SpellCheckerInfo
import android.view.textservice.SpellCheckerSubtype
import android.view.textservice.TextServicesManager

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
		val tsm = textServicesManager() ?: return false
		if (spellCheckingDisabled(tsm)) return false

		// Query what the providers actually support, not the device UI locale —
		// matches iOS/desktop, which probe the spell-check engine.
		val infos = enabledSpellCheckerInfosOrEmpty(tsm)
		if (infos.isEmpty()) return false

		val supported = infos.flatMap { it.supportedLocales() }
		// No enumerable subtypes: provider services locales implicitly. Can't
		// disprove support, and word checks fail open, so allow it.
		if (supported.isEmpty()) return true
		return supported.any { it.language == locale.language.lowercase() }
	}

	actual fun isAvailable(): Boolean {
		// getSystemService is non-null whenever TextServicesManager exists, so it
		// proves nothing; isSpellCheckerEnabled (API 31+) is the real signal.
		val tsm = textServicesManager() ?: return false
		return !spellCheckingDisabled(tsm)
	}

	actual fun currentSystemLocale(): SpLocale {
		val jl = java.util.Locale.getDefault()
		val lang = jl.language.ifBlank { "" }
		val country = jl.country.ifBlank { null }
		return SpLocale(lang, country)
	}

	actual fun availableLocales(): List<SpLocale> {
		val tsm = textServicesManager() ?: return emptyList()
		if (spellCheckingDisabled(tsm)) return emptyList()
		val supported = enabledSpellCheckerInfosOrEmpty(tsm).flatMap { it.supportedLocales() }.distinct()
		if (supported.isEmpty()) return emptyList()

		// Providers advertise every locale they *could* check (~80). Narrow to the
		// languages the user actually uses — system language list plus enabled
		// keyboard subtypes; fall back to the full capability list if there's no overlap.
		val preferred = preferredLanguageCodes()
		if (preferred.isEmpty()) return supported
		val filtered = supported.filter { preferred.contains(it.language.lowercase()) }
		return filtered.ifEmpty { supported }
	}

	private fun preferredLanguageCodes(): Set<String> =
		systemLanguageCodes() + keyboardLanguageCodes()

	private fun systemLanguageCodes(): Set<String> {
		val config = context.resources.configuration
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			val list = config.locales
			(0 until list.size()).mapNotNull { list.get(it).language.lowercase().ifBlank { null } }.toSet()
		} else {
			@Suppress("DEPRECATION")
			setOfNotNull(config.locale?.language?.lowercase()?.ifBlank { null })
		}
	}

	// The user's enabled keyboard languages: distinct from the system UI language
	// list, and the signal users expect (a Spanish keyboard ⇒ Spanish dictionary).
	private fun keyboardLanguageCodes(): Set<String> {
		val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
			?: return emptySet()
		val methods = try {
			imm.enabledInputMethodList
		} catch (_: Throwable) {
			return emptySet()
		}
		return methods.flatMap { imi ->
			val subtypes = try {
				imm.getEnabledInputMethodSubtypeList(imi, true)
			} catch (_: Throwable) {
				emptyList()
			}
			subtypes.mapNotNull { it.languageCodeOrNull() }
		}.toSet()
	}

	@Suppress("DEPRECATION")
	private fun InputMethodSubtype.languageCodeOrNull(): String? {
		val parsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && languageTag.isNotBlank()) {
			java.util.Locale.forLanguageTag(languageTag).takeIf { it.language.isNotBlank() }
				?: legacyLocale(locale)
		} else {
			legacyLocale(locale)
		}
		val lang = parsed.language.lowercase()
		return if (lang.isBlank() || lang == "und") null else lang
	}

	private fun textServicesManager(): TextServicesManager? =
		context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as? TextServicesManager

	// Some OEM ROMs (e.g. ColorOS) ship a TextServicesManager that is missing
	// getEnabledSpellCheckerInfos / isSpellCheckerEnabled, throwing NoSuchMethodError
	// at runtime. Treat any failure as "no spell checkers" rather than crashing the host app.
	private fun enabledSpellCheckerInfosOrEmpty(tsm: TextServicesManager): List<SpellCheckerInfo> =
		try {
			tsm.enabledSpellCheckerInfos.orEmpty()
		} catch (_: Throwable) {
			emptyList()
		}

	// isSpellCheckerEnabled is API 31+; below that we have no signal.
	private fun spellCheckingDisabled(tsm: TextServicesManager): Boolean {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
		return try {
			!tsm.isSpellCheckerEnabled
		} catch (_: Throwable) {
			false
		}
	}

	private fun SpellCheckerInfo.supportedLocales(): List<SpLocale> =
		(0 until subtypeCount).mapNotNull { getSubtypeAt(it).toSpLocaleOrNull() }

	@Suppress("DEPRECATION")
	private fun SpellCheckerSubtype.toSpLocaleOrNull(): SpLocale? {
		// languageTag is BCP-47 (API 24+); forLanguageTag parses script/region
		// subtags (zh-Hant-CN, es-419) correctly. getLocale() is the legacy
		// underscore form (en_US). Fall back when the tag yields no language.
		val parsed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && languageTag.isNotBlank()) {
			java.util.Locale.forLanguageTag(languageTag).takeIf { it.language.isNotBlank() }
				?: legacyLocale(locale)
		} else {
			legacyLocale(locale)
		}
		val lang = parsed.language.lowercase()
		if (lang.isBlank() || lang == "und") return null
		val country = parsed.country.uppercase().ifBlank { null }
		return SpLocale(lang, country)
	}

	private fun legacyLocale(raw: String): java.util.Locale {
		val parts = raw.split('_', '-')
		return java.util.Locale(parts.getOrNull(0).orEmpty(), parts.getOrNull(1).orEmpty())
	}
}
