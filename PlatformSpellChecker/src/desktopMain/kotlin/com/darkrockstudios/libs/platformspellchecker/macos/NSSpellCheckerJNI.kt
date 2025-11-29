package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.LongByReference

/**
 * JNA Interface for the native NSSpellChecker wrapper library.
 * This library uses out-parameters to avoid struct-by-value return issues.
 */
interface NSSpellCheckerJNI : Library {
    companion object {
        val INSTANCE: NSSpellCheckerJNI by lazy {
            // JNA will automatically look in the correct platform-specific directory
            // For macOS ARM64: darwin-aarch64/libNSSpellCheckerJNI.dylib
            // For macOS x86-64: darwin-x86-64/libNSSpellCheckerJNI.dylib
            Native.load("NSSpellCheckerJNI", NSSpellCheckerJNI::class.java)
        }

        // NSNotFound constant
        const val NSNotFound = Long.MAX_VALUE
    }

    /**
     * Check spelling of a string starting at a given offset.
     * @param text The text to check (UTF-8 encoded)
     * @param startingOffset The offset to start checking from
     * @param outLocation Output: location of the first misspelled word (NSNotFound if none)
     * @param outLength Output: length of the misspelled word
     */
    fun checkSpellingOfString(
        text: String,
        startingOffset: Long,
        outLocation: LongByReference,
        outLength: LongByReference
    )

    /**
     * Check spelling with language and wrap options.
     * @param text The text to check (UTF-8 encoded)
     * @param startingOffset The offset to start checking from
     * @param language The language to use for spell checking (can be null)
     * @param wrapFlag Whether to wrap around to the beginning when reaching the end
     * @param outLocation Output: location of the first misspelled word
     * @param outLength Output: length of the misspelled word
     */
    fun checkSpellingOfStringWithOptions(
        text: String,
        startingOffset: Long,
        language: String?,
        wrapFlag: Int,
        outLocation: LongByReference,
        outLength: LongByReference
    )

    /**
     * Find the range of a misspelled word in a string.
     * @param text The text to check (UTF-8 encoded)
     * @param startingOffset The offset to start checking from
     * @param language The language to use (can be null)
     * @param wrapFlag Whether to wrap around
     * @param outLocation Output: location of the misspelled word
     * @param outLength Output: length of the misspelled word
     * @param outWordCount Output: word count (can be null)
     */
    fun rangeOfMisspelledWord(
        text: String,
        startingOffset: Long,
        language: String?,
        wrapFlag: Int,
        outLocation: LongByReference,
        outLength: LongByReference,
        outWordCount: LongByReference?
    )

    /**
     * Get spelling suggestions for a word.
     * @param word The word to get suggestions for (UTF-8 encoded)
     * @param language The language to use (can be null)
     * @return Comma-separated list of suggestions (caller must free with freeMemory)
     */
    fun getSuggestions(word: String, language: String?): Pointer?

    /**
     * Check if a word is in the dictionary (correctly spelled).
     * @param word The word to check (UTF-8 encoded)
     * @param language The language to use (can be null)
     * @return 1 if the word is correct, 0 if misspelled
     */
    fun isWordInDictionary(word: String, language: String?): Int

    /**
     * Learn a new word (add to user dictionary).
     * @param word The word to learn (UTF-8 encoded)
     */
    fun learnWord(word: String)

    /**
     * Unlearn a word (remove from user dictionary).
     * @param word The word to unlearn (UTF-8 encoded)
     */
    fun unlearnWord(word: String)

    /**
     * Ignore a word for the current document/session.
     * @param word The word to ignore (UTF-8 encoded)
     */
    fun ignoreWord(word: String)

    /**
     * Get available languages.
     * @return Comma-separated list of language codes (caller must free with freeMemory)
     */
    fun getAvailableLanguages(): Pointer?

    /**
     * Set the language for spell checking.
     * @param language The language code to set
     * @return 1 if successful, 0 if failed
     */
    fun setLanguage(language: String): Int

    /**
     * Get the current language.
     * @return The current language code (caller must free with freeMemory)
     */
    fun getCurrentLanguage(): Pointer?

    /**
     * Check grammar in a string.
     * @param text The text to check (UTF-8 encoded)
     * @param startingOffset The offset to start checking from
     * @param language The language to use (can be null)
     * @param outLocation Output: location of the grammar error
     * @param outLength Output: length of the grammar error
     */
    fun checkGrammar(
        text: String,
        startingOffset: Long,
        language: String?,
        outLocation: LongByReference,
        outLength: LongByReference
    )

    /**
     * Count continuous spell checking errors from a given offset.
     * @param text The text to check (UTF-8 encoded)
     * @param startingOffset The offset to start from
     * @param language The language to use (can be null)
     * @return The number of errors found
     */
    fun countContinuousSpellCheckingErrors(
        text: String,
        startingOffset: Long,
        language: String?
    ): Long

    /**
     * Free memory allocated by this library.
     * @param ptr The pointer to free
     */
    fun freeMemory(ptr: Pointer)
}

/**
 * Helper extension to convert Pointer to String and free memory
 */
fun Pointer?.toStringAndFree(): String {
    if (this == null) return ""
    return try {
        getString(0, "UTF-8")
    } finally {
        NSSpellCheckerJNI.INSTANCE.freeMemory(this)
    }
}
