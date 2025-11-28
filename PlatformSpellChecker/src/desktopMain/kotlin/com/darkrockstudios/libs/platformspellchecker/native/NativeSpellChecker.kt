package com.darkrockstudios.libs.platformspellchecker.native

/**
 * Common interface for native spell checkers across different operating systems.
 *
 * Implementations:
 * - Windows: Uses the Windows Spell Checking API (Windows 8+)
 * - macOS: Will use NSSpellChecker (to be implemented)
 * - Linux: Will use Hunspell or similar (to be implemented)
 */
interface NativeSpellChecker : AutoCloseable {

    /**
     * The language tag this spell checker was created for (e.g., "en-US").
     */
    val languageTag: String

    /**
     * Checks the spelling of the provided text.
     *
     * @param text The text to check
     * @return List of spelling errors found
     */
    fun checkText(text: String): List<SpellingError>

    /**
     * Gets spelling suggestions for a misspelled word.
     *
     * @param word The misspelled word
     * @return List of suggestions
     */
    fun getSuggestions(word: String): List<String>

    /**
     * Checks if a single word is spelled correctly.
     *
     * @param word The word to check
     * @return true if spelled correctly, false otherwise
     */
    fun isWordCorrect(word: String): Boolean

    /**
     * Adds a word to the user dictionary.
     *
     * @param word The word to add
     */
    fun addToDictionary(word: String)

    /**
     * Ignores a word for this session.
     *
     * @param word The word to ignore
     */
    fun ignoreWord(word: String)
}

/**
 * Represents a spelling error found in text.
 */
data class SpellingError(
    /**
     * The starting character index of the misspelled word in the text.
     */
    val startIndex: Int,

    /**
     * The length of the misspelled word.
     */
    val length: Int,

    /**
     * The type of corrective action suggested.
     */
    val correctiveAction: CorrectiveAction,

    /**
     * Suggested replacement (for auto-correct actions).
     */
    val replacement: String? = null
)

/**
 * Types of corrective actions for spelling errors.
 */
enum class CorrectiveAction {
    /** No action needed (word is correct). */
    NONE,

    /** User should be prompted with suggestions. */
    GET_SUGGESTIONS,

    /** Auto-replace with the provided replacement. */
    REPLACE,

    /** User should be prompted to delete the word. */
    DELETE
}
