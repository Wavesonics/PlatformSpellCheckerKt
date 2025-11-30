package com.darkrockstudios.libs.platformspellchecker.native

import com.darkrockstudios.libs.platformspellchecker.linux.HunspellLibrary
import com.darkrockstudios.libs.platformspellchecker.linux.HunspellWrapper
import io.github.aakira.napier.Napier

/**
 * Linux implementation of NativeSpellChecker using Hunspell.
 *
 * Hunspell is the most widely used spell checker on Linux systems.
 * It's used by LibreOffice, Firefox, Chrome, and many other applications.
 *
 * Dictionary files are searched in standard locations:
 * - /usr/share/hunspell/
 * - /usr/share/myspell/
 * - ~/.local/share/hunspell/
 *
 * User dictionaries are stored in ~/.local/share/hunspell/{lang}_user.dic
 *
 * Reference: https://github.com/hunspell/hunspell
 */
class LinuxSpellChecker private constructor(
	override val languageTag: String,
	private val hunspell: HunspellWrapper
) : NativeSpellChecker {

	override fun checkText(text: String): List<SpellingError> {
		if (text.isBlank()) return emptyList()

		val errors = mutableListOf<SpellingError>()

		// Match words (including apostrophes for contractions like "don't")
		val wordPattern = Regex("""\b[\p{L}']+\b""")

		for (match in wordPattern.findAll(text)) {
			val word = match.value
			val startIndex = match.range.first

			// Skip words that are all uppercase (likely acronyms)
			if (word.all { it.isUpperCase() } && word.length > 1) {
				continue
			}

			// Skip words that are just apostrophes or very short
			if (word.length < 2 || word.all { it == '\'' }) {
				continue
			}

			// Check if the word is correct
			if (!hunspell.isWordCorrect(word)) {
				errors.add(
					SpellingError(
						startIndex = startIndex,
						length = word.length,
						correctiveAction = CorrectiveAction.GET_SUGGESTIONS,
						replacement = null
					)
				)
			}
		}

		return errors
	}

	override fun getSuggestions(word: String): List<String> {
		if (word.isBlank()) return emptyList()
		return hunspell.getSuggestions(word.trim())
	}

	override fun isWordCorrect(word: String): Boolean {
		if (word.isBlank()) return true
		return hunspell.isWordCorrect(word.trim())
	}

	override fun addToDictionary(word: String) {
		if (word.isBlank()) return
		hunspell.addToUserDictionary(word.trim())
	}

	override fun ignoreWord(word: String) {
		if (word.isBlank()) return
		hunspell.ignoreWord(word.trim())
	}

	override fun close() {
		try {
			hunspell.close()
		} catch (e: Exception) {
			Napier.e("Error closing Hunspell: ${e.message}", e)
		}
	}

	companion object {
		/**
		 * Checks if Hunspell is available on this system.
		 */
		fun isAvailable(): Boolean {
			return HunspellLibrary.isAvailable()
		}

		/**
		 * Checks if a language is supported.
		 *
		 * @param languageTag BCP47 language tag (e.g., "en-US")
		 * @return true if supported (dictionary file exists)
		 */
		fun isLanguageSupported(languageTag: String): Boolean {
			return HunspellWrapper.isLanguageSupported(languageTag)
		}

		/**
		 * Creates a spell checker for the specified language.
		 *
		 * @param languageTag BCP47 language tag (e.g., "en-US")
		 * @return LinuxSpellChecker instance, or null if creation failed
		 */
		fun create(languageTag: String): LinuxSpellChecker? {
			return try {
				val wrapper = HunspellWrapper.create(languageTag) ?: return null
				LinuxSpellChecker(languageTag, wrapper)
			} catch (e: Exception) {
				Napier.e("Error creating Linux spell checker: ${e.message}", e)
				null
			}
		}

		/**
		 * Creates a spell checker using the system's default language.
		 *
		 * @return LinuxSpellChecker instance, or null if creation failed
		 */
		fun createDefault(): LinuxSpellChecker? {
			return try {
				val wrapper = HunspellWrapper.createDefault() ?: return null
				LinuxSpellChecker(wrapper.languageTag, wrapper)
			} catch (e: Exception) {
				Napier.e("Error creating default Linux spell checker: ${e.message}", e)
				null
			}
		}

		/**
		 * Gets a list of available languages.
		 *
		 * @return List of BCP47 language tags
		 */
		fun getAvailableLanguages(): List<String> {
			return HunspellWrapper.getAvailableLanguages()
		}

		/**
		 * Gets a list of available dictionary paths to search.
		 */
		fun getDictionaryPaths(): List<String> = HunspellWrapper.DICTIONARY_PATHS +
				(System.getProperty("user.home") + "/.local/share/hunspell")
	}
}
