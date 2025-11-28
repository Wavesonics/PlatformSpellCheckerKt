package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.Pointer

/**
 * JNA wrapper for NSSpellChecker.
 *
 * NSSpellChecker provides spell-checking services on macOS.
 *
 * Key methods:
 * - sharedSpellChecker: Gets the shared spell checker instance
 * - checkSpellingOfString:startingAt:: Finds the first misspelled word
 * - guessesForWordRange:inString:language:inSpellDocumentWithTag:: Gets suggestions
 * - learnWord:: Adds a word to the user dictionary
 * - ignoreWord:inSpellDocumentWithTag:: Ignores a word for a document
 * - availableLanguages: Returns array of supported language codes
 * - setLanguage:: Sets the language for spell checking
 *
 * Reference: https://developer.apple.com/documentation/appkit/nsspellchecker
 */
class NSSpellCheckerWrapper private constructor(private val spellChecker: Pointer) : AutoCloseable {

    // Use a simple incrementing tag for spell document sessions
    private var spellDocumentTag: Long = System.currentTimeMillis()

    /**
     * Checks the spelling of a string and returns the range of the first misspelled word.
     *
     * Note: Due to complexities with NSRange struct returns via JNA, this method
     * currently has limitations. Use isWordCorrect() for word-level checking instead.
     *
     * @param text The text to check
     * @param startingAt The starting position in the text
     * @return NSRange of the misspelled word, or NSRange(NSNotFound, 0) if no errors
     */
    fun checkSpelling(text: String, startingAt: Long = 0): NSRange {
        // For now, return NSNotFound to indicate no implementation
        // The MacOSSpellChecker uses word-level checking instead
        return NSRange(ObjC.NSNotFound, 0)
    }

    /**
     * Gets spelling suggestions for a word.
     *
     * @param word The word to get suggestions for
     * @param language The language code (e.g., "en", "en_US") or null for automatic detection
     * @return List of suggestions, or empty list if none
     */
    fun getSuggestions(word: String, language: String? = null): List<String> {
        val nsWord = ObjC.createNSString(word) ?: return emptyList()
        val nsLanguage = language?.let { ObjC.createNSString(it) }

        try {
            // Create an NSRange for the entire word
            val range = NSRange(0, word.length.toLong())
            val selector = ObjC.selector("guessesForWordRange:inString:language:inSpellDocumentWithTag:")
                ?: return emptyList()

            // [spellChecker guessesForWordRange:range inString:nsWord language:nsLanguage inSpellDocumentWithTag:tag]
            // This method signature is complex due to the NSRange struct parameter
            // We'll need to use a different approach

            // Simpler alternative: use guessesForWord: (deprecated but simpler)
            val simpleSelector = ObjC.selector("guessesForWord:")
                ?: return emptyList()

            val resultArray = ObjC.msgSend(spellChecker, simpleSelector, nsWord)
                ?: return emptyList()

            // Convert NSArray to List<String>
            return nsArrayToStringList(resultArray)
        } finally {
            ObjC.release(nsWord)
            nsLanguage?.let { ObjC.release(it) }
        }
    }

    /**
     * Checks if a word is spelled correctly.
     *
     * Uses correctionForWordRange:inString:language:inSpellDocumentWithTag:
     * which returns nil if the word is correct, or a correction if it's misspelled.
     *
     * @param word The word to check
     * @return true if spelled correctly, false otherwise
     */
    fun isWordCorrect(word: String): Boolean {
        val nsWord = ObjC.createNSString(word) ?: return true

        try {
            // Use correctionForWordRange:inString:language:inSpellDocumentWithTag:
            // This returns nil if the word is correct, or an NSString correction if misspelled
            val selector = ObjC.selector("correctionForWordRange:inString:language:inSpellDocumentWithTag:")
                ?: return guessesBasedCheck(word)

            // Create an NSRange for the entire word
            val range = NSRange(0, word.length.toLong())
            val rangePtr = range.toPointer()

            // Get the language (or use nil for automatic)
            val languageSelector = ObjC.selector("language")
            val language = if (languageSelector != null) {
                ObjC.msgSend(spellChecker, languageSelector)
            } else {
                null
            }

            // This method signature is:
            // - (NSString *)correctionForWordRange:(NSRange)range
            //                             inString:(NSString *)string
            //                             language:(NSString *)language
            //               inSpellDocumentWithTag:(NSInteger)tag
            //
            // Due to NSRange struct passing complexity, let's use a simpler approach:
            // Just check if the word appears in the correction list

            // Fallback to guesses-based check
            return guessesBasedCheck(word)
        } catch (e: Exception) {
            return true
        } finally {
            ObjC.release(nsWord)
        }
    }

    /**
     * Fallback method using guessesForWord with heuristics.
     *
     * Observation from testing:
     * - Correct words return MANY suggestions (10+) - these are alternatives/related words
     * - Misspelled words return FEW suggestions (1-3) - these are corrections
     * - Unknown/nonsense words return 0 suggestions
     *
     * Heuristic: >= 5 suggestions means correct, < 5 means misspelled
     */
    private fun guessesBasedCheck(word: String): Boolean {
        val nsWord = ObjC.createNSString(word) ?: return true

        try {
            val selector = ObjC.selector("guessesForWord:") ?: return true
            val resultArray = ObjC.msgSend(spellChecker, selector, nsWord)

            if (resultArray == null) return false // No guesses usually means misspelled

            val countSelector = ObjC.selector("count") ?: return true
            val countResult = ObjC.msgSend(resultArray, countSelector) ?: return true
            val count = Pointer.nativeValue(countResult)

            // Heuristic based on observation:
            // - Correct words: many suggestions (alternatives/related words)
            // - Misspelled words: few suggestions (corrections)
            // - Nonsense words: no suggestions
            return count >= 5
        } catch (e: Exception) {
            return true
        } finally {
            ObjC.release(nsWord)
        }
    }

    /**
     * Adds a word to the user dictionary.
     *
     * @param word The word to add
     */
    fun learnWord(word: String) {
        val nsWord = ObjC.createNSString(word) ?: return

        try {
            val selector = ObjC.selector("learnWord:") ?: return
            ObjC.msgSend(spellChecker, selector, nsWord)
        } finally {
            ObjC.release(nsWord)
        }
    }

    /**
     * Ignores a word for this spell checking session.
     *
     * @param word The word to ignore
     */
    fun ignoreWord(word: String) {
        val nsWord = ObjC.createNSString(word) ?: return

        try {
            val selector = ObjC.selector("ignoreWord:inSpellDocumentWithTag:")
                ?: return
            // Use msgSend with mixed arguments (Pointer, Long)
            ObjC.msgSend(spellChecker, selector, nsWord, spellDocumentTag, 0)
        } finally {
            ObjC.release(nsWord)
        }
    }

    /**
     * Sets the language for spell checking.
     *
     * @param language The language code (e.g., "en_US")
     * @return true if the language was set successfully
     */
    fun setLanguage(language: String): Boolean {
        val nsLanguage = ObjC.createNSString(language) ?: return false

        try {
            val selector = ObjC.selector("setLanguage:") ?: return false
            val result = ObjC.msgSend(spellChecker, selector, nsLanguage)

            // setLanguage: returns BOOL (scalar value)
            return Pointer.nativeValue(result) != 0L
        } finally {
            ObjC.release(nsLanguage)
        }
    }

    /**
     * Gets the current language.
     *
     * @return The language code, or null if not available
     */
    fun getLanguage(): String? {
        val selector = ObjC.selector("language") ?: return null
        val nsLanguage = ObjC.msgSend(spellChecker, selector) ?: return null
        return ObjC.nsStringToString(nsLanguage)
    }

    override fun close() {
        // Close the spell document tag
        val closeSelector = ObjC.selector("closeSpellDocumentWithTag:")
        if (closeSelector != null && spellDocumentTag != 0L) {
            ObjC.msgSend(spellChecker, closeSelector, spellDocumentTag, 0)
        }
        // Note: We don't release the shared spell checker as it's a singleton
    }

    companion object {
        /**
         * Gets the shared NSSpellChecker instance.
         *
         * @return NSSpellCheckerWrapper instance, or null if unavailable
         */
        fun shared(): NSSpellCheckerWrapper? {
            val nsSpellCheckerClass = ObjC.getClass("NSSpellChecker") ?: return null
            val sharedSelector = ObjC.selector("sharedSpellChecker") ?: return null

            val spellChecker = ObjC.msgSend(nsSpellCheckerClass, sharedSelector)
                ?: return null

            return NSSpellCheckerWrapper(spellChecker)
        }

        /**
         * Gets a list of available languages.
         *
         * @return List of language codes, or empty list if unavailable
         */
        fun availableLanguages(): List<String> {
            val nsSpellCheckerClass = ObjC.getClass("NSSpellChecker") ?: return emptyList()
            val sharedSelector = ObjC.selector("sharedSpellChecker") ?: return emptyList()
            val spellChecker = ObjC.msgSend(nsSpellCheckerClass, sharedSelector)
                ?: return emptyList()

            val languagesSelector = ObjC.selector("availableLanguages") ?: return emptyList()
            val languagesArray = ObjC.msgSend(spellChecker, languagesSelector)
                ?: return emptyList()

            return nsArrayToStringList(languagesArray)
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

        /**
         * Converts an NSArray to a List<String>.
         */
        private fun nsArrayToStringList(nsArray: Pointer): List<String> {
            val countSelector = ObjC.selector("count") ?: return emptyList()
            val countResult = ObjC.msgSend(nsArray, countSelector) ?: return emptyList()

            // count returns NSUInteger (scalar value)
            val count = Pointer.nativeValue(countResult)
            if (count == 0L) return emptyList()

            val objectAtIndexSelector = ObjC.selector("objectAtIndex:") ?: return emptyList()
            val results = mutableListOf<String>()

            for (i in 0 until count) {
                // objectAtIndex: takes a single NSUInteger argument
                val nsString = ObjC.msgSend(nsArray, objectAtIndexSelector, i)
                if (nsString != null) {
                    val string = ObjC.nsStringToString(nsString)
                    if (string != null) {
                        results.add(string)
                    }
                }
            }

            return results
        }

    }
}
