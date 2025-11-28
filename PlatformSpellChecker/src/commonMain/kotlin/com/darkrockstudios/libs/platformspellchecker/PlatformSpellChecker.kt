package com.darkrockstudios.libs.platformspellchecker

/**
 * Platform-specific spell checker that provides spell checking functionality.
 *
 * On Android, this uses the system's TextServicesManager and SpellCheckerSession.
 * On Desktop, this is currently stubbed and returns placeholder results.
 */
expect class PlatformSpellChecker {
    /**
     * Performs spell check on a sentence or multi-word text.
     * Returns a list of suggestions in the format "'misspelledWord' → 'suggestion'".
     * Returns "No spelling errors found" if the text is correctly spelled.
     */
    suspend fun performSpellCheck(text: String): List<String>

    /**
     * Checks a single word for spelling errors.
     * Returns spelling suggestions if the word is misspelled,
     * or "'word' is correctly spelled" if the word is correct.
     */
    suspend fun checkWord(word: String): List<String>
}
