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
     */
    suspend fun performSpellCheck(text: String): List<SpellingCorrection>

    /**
     * Checks a single word for spelling errors and returns spelling suggestions.
     * Implementations should return an empty list when the word is already correct
     * or when no suggestions are available. The number of returned suggestions
     * should not exceed [maxSuggestions].
     */
    suspend fun checkWord(word: String, maxSuggestions: Int = 5): List<String>

	/**
	 * Checks if a single word exists in the active dictionary and is considered correctly spelled.
	 * Returns true when the word is recognized by the platform spell checker; false otherwise.
     */
	suspend fun isWordCorrect(word: String): Boolean

	/**
	 * Releases any platform resources held by the spell checker (if applicable).
	 * Safe to call multiple times. No-op on platforms that do not require cleanup.
	 */
	fun close()
}
