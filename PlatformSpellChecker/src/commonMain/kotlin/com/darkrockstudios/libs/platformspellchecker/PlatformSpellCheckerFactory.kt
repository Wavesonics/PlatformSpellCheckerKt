package com.darkrockstudios.libs.platformspellchecker

/**
 * PlatformSpellCheckerFactory creates platform spell checkers and provides a capability check.
 *
 * Notes:
 * - Android: the actual implementation exposes a platform-only `initialize(context)` you can call
 *   from `Application.onCreate` to enable capability checks that require a Context.
 * - iOS/Desktop: no initialization required.
 */
expect class PlatformSpellCheckerFactory {
	/**
	 * Creates a [PlatformSpellChecker].
	 * If [locale] is null, the user's current/system language is used.
	 * If [locale] is non-null, implementations should validate support and throw if unsupported.
	 * @param locale BCP47 language tag (e.g., "en-US")
	 * @throws IllegalArgumentException if [locale] is unsupported
	 */
	suspend fun createSpellChecker(locale: SpLocale? = null): PlatformSpellChecker

	/**
	 * @param locale BCP47 language tag (e.g., "en-US")
	 * @return true if the given [locale] appears to be supported (dictionary available) on this device.
	 */
	fun hasLanguage(locale: SpLocale): Boolean

	/**
	 * @return true if spell checking is available on this platform at runtime.
	 * Useful for feature gating.
	 */
	fun isAvailable(): Boolean

	/**
	 * @return the user's current/system locale in the [SpLocale] format.
	 */
	fun currentSystemLocale(): SpLocale

	/**
	 * @return a list of locales that appear to be available for spell checking on this device.
	 * Implementations may return a best-effort list.
	 */
	fun availableLocales(): List<SpLocale>
}
