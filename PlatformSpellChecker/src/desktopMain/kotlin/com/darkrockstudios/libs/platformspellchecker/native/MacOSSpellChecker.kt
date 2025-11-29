package com.darkrockstudios.libs.platformspellchecker.native

import com.darkrockstudios.libs.platformspellchecker.macos.NSSpellCheckerWrapper
import io.github.aakira.napier.Napier
import java.util.Locale

/**
 * macOS implementation of NativeSpellChecker.
 *
 * Uses NSSpellChecker via JNA bindings to provide spell checking on macOS.
 *
 * NSSpellChecker provides:
 * - checkSpellingOfString:startingAt: for spell checking
 * - guessesForWord: for suggestions
 * - learnWord: for adding to dictionary
 * - ignoreWord:inSpellDocumentWithTag: for ignoring words
 *
 * Reference: https://developer.apple.com/documentation/appkit/nsspellchecker
 */
class MacOSSpellChecker private constructor(
    override val languageTag: String,
    private val spellChecker: NSSpellCheckerWrapper
) : NativeSpellChecker {

    private val ignoredWords = mutableSetOf<String>()

    override fun checkText(text: String): List<SpellingError> {
        if (text.isBlank()) return emptyList()

        val errors = mutableListOf<SpellingError>()

        try {
            var startingPosition = 0L

            // Use the proper checkSpelling method now that NSRange returns work
            while (startingPosition < text.length) {
                val range = spellChecker.checkSpelling(text, startingPosition)

                // Check if no more misspellings found
                if (range.notFound) {
                    break
                }

                // Get the misspelled word
                val misspelledWord = text.substring(range.location.toInt(),
                                                   (range.location + range.length).toInt())

                // Skip if word is in ignored list
                if (!ignoredWords.contains(misspelledWord.lowercase())) {
                    errors.add(
                        SpellingError(
                            startIndex = range.location.toInt(),
                            length = range.length.toInt(),
                            correctiveAction = CorrectiveAction.GET_SUGGESTIONS,
                            replacement = null
                        )
                    )
                }

                // Move to the next position after this word
                startingPosition = range.location + range.length
            }

            return errors
        } catch (e: Exception) {
            Napier.e("Error checking text: ${e.message}", e)
            return emptyList()
        }
    }

    override fun getSuggestions(word: String): List<String> {
        if (word.isBlank()) return emptyList()

        return try {
            spellChecker.getSuggestions(word.trim(), languageTag)
        } catch (e: Exception) {
            Napier.e("Error getting suggestions: ${e.message}", e)
            emptyList()
        }
    }

    override fun isWordCorrect(word: String): Boolean {
        if (word.isBlank()) return true

        val cleanWord = word.trim()
        if (ignoredWords.contains(cleanWord.lowercase())) {
            return true
        }

        return try {
            spellChecker.isWordCorrect(cleanWord)
        } catch (e: Exception) {
            Napier.e("Error checking word: ${e.message}", e)
            true
        }
    }

    override fun addToDictionary(word: String) {
        if (word.isBlank()) return

        try {
            spellChecker.learnWord(word.trim())
        } catch (e: Exception) {
            Napier.e("Error adding word to dictionary: ${e.message}", e)
        }
    }

    override fun ignoreWord(word: String) {
        if (word.isBlank()) return

        try {
            val cleanWord = word.trim()
            ignoredWords.add(cleanWord.lowercase())
            spellChecker.ignoreWord(cleanWord)
        } catch (e: Exception) {
            Napier.e("Error ignoring word: ${e.message}", e)
        }
    }

    override fun close() {
        try {
            spellChecker.close()
        } catch (e: Exception) {
            Napier.e("Error closing spell checker: ${e.message}", e)
        }
    }

    companion object {
        /**
         * Checks if a language is supported.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return true if supported
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            return try {
                NSSpellCheckerWrapper.isLanguageSupported(languageTag)
            } catch (e: Exception) {
                Napier.e("Error checking language support: ${e.message}", e)
                false
            }
        }

        /**
         * Creates a spell checker for the specified language.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return MacOSSpellChecker instance, or null if creation failed
         */
        fun create(languageTag: String): MacOSSpellChecker? {
            return try {
                // Normalize the language tag (NSSpellChecker uses underscores)
                val normalizedTag = languageTag.replace("-", "_")

                val wrapper = NSSpellCheckerWrapper.shared() ?: return null

                // Try to set the language
                if (!wrapper.setLanguage(normalizedTag)) {
                    // If exact match fails, try just the language part
                    val languagePart = normalizedTag.split("_").firstOrNull()
                    if (languagePart != null && !wrapper.setLanguage(languagePart)) {
                        Napier.w("Could not set language to $languageTag, using default")
                    }
                }

                MacOSSpellChecker(languageTag, wrapper)
            } catch (e: Exception) {
                Napier.e("Error creating macOS spell checker: ${e.message}", e)
                null
            }
        }

        /**
         * Creates a spell checker using the system's default language.
         *
         * @return MacOSSpellChecker instance, or null if creation failed
         */
        fun createDefault(): MacOSSpellChecker? {
            return try {
                val wrapper = NSSpellCheckerWrapper.shared() ?: return null

                // Get the current language from the spell checker
                val currentLanguage = wrapper.getLanguage()
                val languageTag = currentLanguage
                    ?: Locale.getDefault().toLanguageTag()

                MacOSSpellChecker(languageTag, wrapper)
            } catch (e: Exception) {
                Napier.e("Error creating default macOS spell checker: ${e.message}", e)
                null
            }
        }

        /**
         * Gets a list of available languages.
         *
         * @return List of language codes
         */
        fun getAvailableLanguages(): List<String> {
            return try {
                NSSpellCheckerWrapper.availableLanguages()
            } catch (e: Exception) {
                Napier.e("Error getting available languages: ${e.message}", e)
                emptyList()
            }
        }
    }
}
