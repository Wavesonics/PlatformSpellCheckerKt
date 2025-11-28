package com.darkrockstudios.libs.platformspellchecker.native

/**
 * macOS implementation of NativeSpellChecker.
 *
 * TODO: Implement using NSSpellChecker via JNA or JNI.
 * NSSpellChecker provides:
 * - checkSpellingOfString:startingAt: for spell checking
 * - guessesForWordRange:inString:language:inSpellDocumentWithTag: for suggestions
 * - learnWord: for adding to dictionary
 * - ignoreWord:inSpellDocumentWithTag: for ignoring words
 *
 * Reference: https://developer.apple.com/documentation/appkit/nsspellchecker
 */
class MacOSSpellChecker private constructor(
    override val languageTag: String
) : NativeSpellChecker {

    override fun checkText(text: String): List<SpellingError> {
        // TODO: Implement using NSSpellChecker
        return emptyList()
    }

    override fun getSuggestions(word: String): List<String> {
        // TODO: Implement using NSSpellChecker
        return emptyList()
    }

    override fun isWordCorrect(word: String): Boolean {
        // TODO: Implement using NSSpellChecker
        return true
    }

    override fun addToDictionary(word: String) {
        // TODO: Implement using NSSpellChecker
    }

    override fun ignoreWord(word: String) {
        // TODO: Implement using NSSpellChecker
    }

    override fun close() {
        // TODO: Clean up resources
    }

    companion object {
        /**
         * Checks if a language is supported.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return true if supported
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            // TODO: Query NSSpellChecker.availableLanguages
            return false
        }

        /**
         * Creates a spell checker for the specified language.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return MacOSSpellChecker instance, or null if creation failed
         */
        fun create(languageTag: String): MacOSSpellChecker? {
            // TODO: Implement
            return null
        }

        /**
         * Creates a spell checker using the system's default language.
         *
         * @return MacOSSpellChecker instance, or null if creation failed
         */
        fun createDefault(): MacOSSpellChecker? {
            // TODO: Implement using NSSpellChecker.automaticallyIdentifiesLanguages
            return null
        }
    }
}
