package com.darkrockstudios.libs.platformspellchecker.native

/**
 * Linux implementation of NativeSpellChecker.
 *
 * TODO: Implement using one of the following options:
 *
 * Option 1: Hunspell (most common)
 * - Use JNA to bind to libhunspell
 * - Hunspell_create, Hunspell_spell, Hunspell_suggest, Hunspell_add, etc.
 * - Dictionary files typically in /usr/share/hunspell/
 * - Reference: https://github.com/hunspell/hunspell
 *
 * Option 2: Enchant (meta spell-checker)
 * - Provides unified API across multiple backends (Hunspell, Aspell, etc.)
 * - Use JNA to bind to libenchant
 * - Reference: https://abiword.github.io/enchant/
 *
 * Option 3: Aspell
 * - Use JNA to bind to libaspell
 * - Reference: http://aspell.net/
 */
class LinuxSpellChecker private constructor(
    override val languageTag: String
) : NativeSpellChecker {

    override fun checkText(text: String): List<SpellingError> {
        // TODO: Implement using Hunspell/Enchant
        return emptyList()
    }

    override fun getSuggestions(word: String): List<String> {
        // TODO: Implement using Hunspell/Enchant
        return emptyList()
    }

    override fun isWordCorrect(word: String): Boolean {
        // TODO: Implement using Hunspell/Enchant
        return true
    }

    override fun addToDictionary(word: String) {
        // TODO: Implement using Hunspell/Enchant
    }

    override fun ignoreWord(word: String) {
        // TODO: Implement - may need session-based ignore list
    }

    override fun close() {
        // TODO: Clean up Hunspell handle
    }

    companion object {
        /**
         * Checks if a language is supported.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return true if supported (dictionary file exists)
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            // TODO: Check if dictionary files exist for this language
            // Typical paths:
            // - /usr/share/hunspell/{lang}.dic and {lang}.aff
            // - /usr/share/myspell/{lang}.dic and {lang}.aff
            return false
        }

        /**
         * Creates a spell checker for the specified language.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return LinuxSpellChecker instance, or null if creation failed
         */
        fun create(languageTag: String): LinuxSpellChecker? {
            // TODO: Implement
            return null
        }

        /**
         * Creates a spell checker using the system's default language.
         *
         * @return LinuxSpellChecker instance, or null if creation failed
         */
        fun createDefault(): LinuxSpellChecker? {
            // TODO: Use LANG environment variable or locale settings
            return null
        }

        /**
         * Gets a list of available dictionary paths to search.
         */
        fun getDictionaryPaths(): List<String> = listOf(
            "/usr/share/hunspell",
            "/usr/share/myspell",
            "/usr/share/myspell/dicts",
            System.getProperty("user.home") + "/.local/share/hunspell"
        )
    }
}
