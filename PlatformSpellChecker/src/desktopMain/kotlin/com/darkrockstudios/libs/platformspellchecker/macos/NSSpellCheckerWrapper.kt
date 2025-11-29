package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.ptr.LongByReference

/**
 * Wrapper for NSSpellChecker using the native JNI library.
 *
 * This implementation uses a native Objective-C wrapper library that handles
 * NSRange struct returns via out-parameters, solving JNA's limitations with
 * struct-by-value returns.
 *
 * Key methods:
 * - checkSpelling: Finds the first misspelled word in text
 * - getSuggestions: Gets spelling suggestions for a word
 * - isWordCorrect: Checks if a word is spelled correctly
 * - learnWord: Adds a word to the user dictionary
 * - ignoreWord: Ignores a word for the current session
 * - availableLanguages: Returns array of supported language codes
 * - setLanguage: Sets the language for spell checking
 *
 * Reference: https://developer.apple.com/documentation/appkit/nsspellchecker
 */
class NSSpellCheckerWrapper : AutoCloseable {

    private val jni = NSSpellCheckerJNI.INSTANCE

    /**
     * Checks the spelling of a string and returns the range of the first misspelled word.
     *
     * @param text The text to check
     * @param startingAt The starting position in the text
     * @return NSRange of the misspelled word, or NSRange(NSNotFound, 0) if no errors
     */
    fun checkSpelling(text: String, startingAt: Long = 0): NSRange {
        val outLocation = LongByReference()
        val outLength = LongByReference()

        jni.checkSpellingOfString(text, startingAt, outLocation, outLength)

        return NSRange(outLocation.value, outLength.value)
    }

    /**
     * Checks the spelling of a string with language options.
     *
     * @param text The text to check
     * @param startingAt The starting position in the text
     * @param language The language code (e.g., "en", "en_US") or null for automatic detection
     * @param wrap Whether to wrap around when reaching the end
     * @return NSRange of the misspelled word, or NSRange(NSNotFound, 0) if no errors
     */
    fun checkSpellingWithOptions(
        text: String,
        startingAt: Long = 0,
        language: String? = null,
        wrap: Boolean = false
    ): NSRange {
        val outLocation = LongByReference()
        val outLength = LongByReference()

        jni.checkSpellingOfStringWithOptions(
            text, startingAt, language, if (wrap) 1 else 0,
            outLocation, outLength
        )

        return NSRange(outLocation.value, outLength.value)
    }

    /**
     * Gets spelling suggestions for a word.
     *
     * @param word The word to get suggestions for
     * @param language The language code (e.g., "en", "en_US") or null for automatic detection
     * @return List of suggestions, or empty list if none
     */
    fun getSuggestions(word: String, language: String? = null): List<String> {
        val suggestionsPtr = jni.getSuggestions(word, language)
        val suggestionsStr = suggestionsPtr.toStringAndFree()

        return if (suggestionsStr.isEmpty()) {
            emptyList()
        } else {
            suggestionsStr.split(",")
        }
    }

    /**
     * Checks if a word is spelled correctly.
     *
     * @param word The word to check
     * @param language The language code or null for automatic detection
     * @return true if spelled correctly, false otherwise
     */
    fun isWordCorrect(word: String, language: String? = null): Boolean {
        return jni.isWordInDictionary(word, language) == 1
    }

    /**
     * Adds a word to the user dictionary.
     *
     * @param word The word to add
     */
    fun learnWord(word: String) {
        jni.learnWord(word)
    }

    /**
     * Removes a word from the user dictionary.
     *
     * @param word The word to remove
     */
    fun unlearnWord(word: String) {
        jni.unlearnWord(word)
    }

    /**
     * Ignores a word for this spell checking session.
     *
     * @param word The word to ignore
     */
    fun ignoreWord(word: String) {
        jni.ignoreWord(word)
    }

    /**
     * Sets the language for spell checking.
     *
     * @param language The language code (e.g., "en_US")
     * @return true if the language was set successfully
     */
    fun setLanguage(language: String): Boolean {
        return jni.setLanguage(language) == 1
    }

    /**
     * Gets the current language.
     *
     * @return The language code, or null if not available
     */
    fun getLanguage(): String? {
        val langPtr = jni.getCurrentLanguage()
        return langPtr.toStringAndFree().ifEmpty { null }
    }

    /**
     * Find the range of a misspelled word in a string.
     *
     * @param text The text to check
     * @param startingAt The starting position
     * @param language The language code or null for automatic detection
     * @param wrap Whether to wrap around
     * @return NSRange of the misspelled word
     */
    fun rangeOfMisspelledWord(
        text: String,
        startingAt: Long = 0,
        language: String? = null,
        wrap: Boolean = false
    ): NSRange {
        val outLocation = LongByReference()
        val outLength = LongByReference()
        val outWordCount = LongByReference()

        jni.rangeOfMisspelledWord(
            text, startingAt, language, if (wrap) 1 else 0,
            outLocation, outLength, outWordCount
        )

        return NSRange(outLocation.value, outLength.value)
    }

    /**
     * Check grammar in a string.
     *
     * @param text The text to check
     * @param startingAt The starting position
     * @param language The language code or null for automatic detection
     * @return NSRange of the grammar error
     */
    fun checkGrammar(
        text: String,
        startingAt: Long = 0,
        language: String? = null
    ): NSRange {
        val outLocation = LongByReference()
        val outLength = LongByReference()

        jni.checkGrammar(text, startingAt, language, outLocation, outLength)

        return NSRange(outLocation.value, outLength.value)
    }

    /**
     * Count continuous spell checking errors from a given offset.
     *
     * @param text The text to check
     * @param startingAt The offset to start from
     * @param language The language code or null for automatic detection
     * @return The number of errors found
     */
    fun countSpellCheckingErrors(
        text: String,
        startingAt: Long = 0,
        language: String? = null
    ): Long {
        return jni.countContinuousSpellCheckingErrors(text, startingAt, language)
    }

    override fun close() {
        // No special cleanup needed for native library
    }

    companion object {
        /**
         * Gets the shared NSSpellChecker instance wrapper.
         *
         * @return NSSpellCheckerWrapper instance
         */
        fun shared(): NSSpellCheckerWrapper {
            return NSSpellCheckerWrapper()
        }

        /**
         * Gets a list of available languages.
         *
         * @return List of language codes, or empty list if unavailable
         */
        fun availableLanguages(): List<String> {
            val langPtr = NSSpellCheckerJNI.INSTANCE.getAvailableLanguages()
            val langStr = langPtr.toStringAndFree()

            return if (langStr.isEmpty()) {
                emptyList()
            } else {
                langStr.split(",")
            }
        }

        /**
         * Checks if a language is supported.
         *
         * @param languageTag The language tag to check
         * @return true if supported
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            val available = availableLanguages()

            // Try exact match first
            if (available.contains(languageTag)) return true

            // Try with underscores converted to hyphens (e.g., "en-US" vs "en_US")
            val normalized = languageTag.replace("-", "_")
            if (available.contains(normalized)) return true

            // Try just the language part (e.g., "en" from "en-US")
            val languagePart = languageTag.split("-", "_").firstOrNull()
            if (languagePart != null && available.any { it.startsWith(languagePart) }) {
                return true
            }

            return false
        }
    }
}