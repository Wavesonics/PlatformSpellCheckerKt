package com.darkrockstudios.libs.platformspellchecker

/**
 * Scope of a custom-dictionary mutation.
 *
 * Spell-check results always treat [AppLocal] words as correct on every platform.
 * [System] additionally delegates to the platform's native learn/unlearn mechanism
 * where one exists.
 */
enum class DictionaryScope {
	/**
	 * In-memory, app-private. Persists only for the lifetime of this
	 * [PlatformSpellChecker] instance. Callers that want cross-session
	 * persistence should snapshot via [PlatformSpellChecker.userDictionary] and
	 * re-seed on startup.
	 */
	AppLocal,

	/**
	 * Uses the platform's native user-dictionary mechanism, which persists
	 * across app restarts. Scope of that persistence varies:
	 *  - iOS / macOS: OS-wide (shared with every app on the device).
	 *  - Windows: per-user, per-language Windows dictionary.
	 *  - Linux (Hunspell): per-user file at `~/.local/share/hunspell/{lang}_user.dic`.
	 *  - Android: no native API exists; falls back to [AppLocal] with a warning.
	 *
	 * Removal support is best-effort and not available everywhere (e.g. Windows
	 * basic ISpellChecker does not expose remove).
	 */
	System,
}
