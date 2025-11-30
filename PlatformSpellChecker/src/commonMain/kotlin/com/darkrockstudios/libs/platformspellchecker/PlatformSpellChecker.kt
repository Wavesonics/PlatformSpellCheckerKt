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
     * Checks a single word for spelling errors.
     * Returns spelling suggestions if the word is misspelled,
     * or "'word' is correctly spelled" if the word is correct.
     */
    suspend fun checkWord(word: String): List<String>

    
}
