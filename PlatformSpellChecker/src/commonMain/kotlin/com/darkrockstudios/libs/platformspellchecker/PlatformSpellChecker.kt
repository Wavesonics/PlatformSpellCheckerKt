package com.darkrockstudios.libs.platformspellchecker

/**
 * Platform-specific spell checker that provides spell checking functionality.
 *
 * On Android, this uses the system's TextServicesManager and SpellCheckerSession.
 * On Desktop, this uses native spell checking APIs (Windows Spell Checking API, macOS NSSpellChecker, or Hunspell on Linux).
 */
expect class PlatformSpellChecker {
	/**
	 * Performs spell check on a sentence or multi-word text.
	 * Returns a list of [SpellingCorrection] objects containing the misspelled words,
	 * their positions in the original text, and suggested corrections.
	 * Returns an empty list if no spelling errors are found.
	 *
	 * Words added via [addToDictionary] (any scope) and words passed to
	 * [ignoreWord] are filtered out of the results.
	 */
	suspend fun checkMultiword(text: String): List<SpellingCorrection>

	/**
	 * Checks a single word and returns a structured result indicating if it is spelled correctly.
	 * When misspelled, includes up to [maxSuggestions] suggestions (may be empty).
	 *
	 * Words in the user dictionary (see [addToDictionary]) or marked via
	 * [ignoreWord] are reported as [CorrectWord].
	 */
	suspend fun checkWord(word: String, maxSuggestions: Int = 5): WordCheckResult

	/**
	 * Checks if a single word exists in the active dictionary and is considered correctly spelled.
	 * Returns true when the word is recognized by the platform spell checker or appears in the
	 * user dictionary (see [addToDictionary]) or session ignores (see [ignoreWord]); false otherwise.
	 */
	suspend fun isWordCorrect(word: String): Boolean

	/**
	 * Adds [word] to the user dictionary. Subsequent spell checks will treat it
	 * as correctly spelled.
	 *
	 * @param scope Controls persistence semantics. See [DictionaryScope] for details.
	 * Defaults to [DictionaryScope.AppLocal] for predictability across platforms.
	 */
	suspend fun addToDictionary(word: String, scope: DictionaryScope = DictionaryScope.AppLocal)

	/**
	 * Removes [word] from the user dictionary previously added via [addToDictionary].
	 *
	 * For [DictionaryScope.System], removal is best-effort: supported on iOS,
	 * macOS, and Linux (Hunspell). On Windows the basic spell-check API does not
	 * expose a remove operation; the call is a no-op and logs a warning. On
	 * Android it falls back to [DictionaryScope.AppLocal].
	 */
	suspend fun removeFromDictionary(word: String, scope: DictionaryScope = DictionaryScope.AppLocal)

	/**
	 * Marks [word] as ignored for the lifetime of this checker. Unlike
	 * [addToDictionary], ignores are not persisted and are not returned by
	 * [userDictionary]. Where the platform provides a native ignore facility
	 * (iOS, macOS, Windows, Linux/Hunspell) it is used; otherwise the word is
	 * filtered out of results in this library.
	 */
	suspend fun ignoreWord(word: String)

	/**
	 * @return a snapshot of the words currently in the app-local user dictionary.
	 * Useful for persisting the dictionary between app sessions. Does not include
	 * words added with [DictionaryScope.System] on platforms with native learn
	 * support, since those are owned by the OS.
	 */
	fun userDictionary(): Set<String>

	/**
	 * Releases any platform resources held by the spell checker (if applicable).
	 * Safe to call multiple times. No-op on platforms that do not require cleanup.
	 */
	fun close()
}
