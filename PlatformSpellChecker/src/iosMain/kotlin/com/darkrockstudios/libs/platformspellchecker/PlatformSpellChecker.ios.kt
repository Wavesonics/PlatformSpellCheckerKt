package com.darkrockstudios.libs.platformspellchecker

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.*
import platform.UIKit.UITextChecker

/**
 * iOS implementation of PlatformSpellChecker using UITextChecker.
 *
 * UITextChecker is part of UIKit and provides spell checking functionality
 * using the system's dictionaries and language settings.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformSpellChecker(
	private val locale: SpLocale? = null
) : AutoCloseable {

	private val textChecker = UITextChecker()
	private val userDict = UserDictionary()
	private val language: String by lazy {
		// Determine the language tag for UITextChecker (expects underscore format like en_US)
		if (locale != null) {
			if (locale.country.isNullOrBlank()) locale.language else "${locale.language}_${locale.country}"
		} else {
			// Use system preferred language and convert from BCP47 (en-US) to underscore (en_US)
			val preferred = (NSLocale.preferredLanguages.firstOrNull() as? String)
				?: NSLocale.currentLocale.localeIdentifier
			preferred.replace('-', '_')
		}
	}

	/**
	 * Performs spell check on a sentence or multi-word text.
	 * Returns a list of [SpellingCorrection] objects containing the misspelled words,
	 * their positions in the original text, and suggested corrections.
	 * Returns an empty list if no spelling errors are found.
	 */
	actual suspend fun checkMultiword(text: String): List<SpellingCorrection> {
		if (text.isBlank()) {
			return emptyList()
		}

		val results = mutableListOf<SpellingCorrection>()
		val nsString = text as NSString
		val range = NSMakeRange(0u, text.length.toULong())

		var currentOffset = 0uL

		while (currentOffset < text.length.toULong()) {
			val searchRange = NSMakeRange(currentOffset, text.length.toULong() - currentOffset)

			val misspelledRange = textChecker.rangeOfMisspelledWordInString(
				stringToCheck = text,
				range = searchRange,
				startingAt = currentOffset.toLong(),
				wrap = false,
				language = language
			)

			// If no misspelled word found, break
			if (misspelledRange.useContents { location } == NSNotFound.toULong()) {
				break
			}

			// Extract the misspelled word
			val misspelledWord = nsString.substringWithRange(misspelledRange)

			// Get suggestions for the misspelled word
			val suggestions = textChecker.guessesForWordRange(
				range = misspelledRange,
				inString = text,
				language = language
			) as? List<*>

			val suggestionList = suggestions?.mapNotNull { it as? String } ?: emptyList()

			if (!userDict.isKnown(misspelledWord)) {
				val correction = SpellingCorrection(
					misspelledWord = misspelledWord,
					startIndex = misspelledRange.useContents { location.toInt() },
					length = misspelledRange.useContents { length.toInt() },
					suggestions = suggestionList
				)
				results.add(correction)
			}

			// Move to the next position after this misspelled word
			currentOffset = misspelledRange.useContents { location + length }
		}

		return results
	}

	actual suspend fun checkWord(word: String, maxSuggestions: Int): WordCheckResult {
		val trimmed = word.trim()
		if (trimmed.isEmpty() || trimmed.contains(" ")) return CorrectWord(trimmed)
		if (userDict.isKnown(trimmed)) return CorrectWord(trimmed)

		val range = NSMakeRange(0u, trimmed.length.toULong())

		val misspelledRange = textChecker.rangeOfMisspelledWordInString(
			stringToCheck = trimmed,
			range = range,
			startingAt = 0,
			wrap = false,
			language = language
		)

		if (misspelledRange.useContents { location } == NSNotFound.toULong()) {
			return CorrectWord(trimmed)
		}

		val suggestions = textChecker.guessesForWordRange(
			range = misspelledRange,
			inString = trimmed,
			language = language
		)

		val max = if (maxSuggestions <= 0) 5 else maxSuggestions
		val list = suggestions?.mapNotNull { it as? String }?.take(max) ?: emptyList()
		return MisspelledWord(trimmed, list)
	}

	actual suspend fun isWordCorrect(word: String): Boolean {
		if (word.isBlank()) return false
		if (userDict.isKnown(word.trim())) return true

		val range = NSMakeRange(0u, word.length.toULong())
		val misspelledRange = textChecker.rangeOfMisspelledWordInString(
			stringToCheck = word,
			range = range,
			startingAt = 0,
			wrap = false,
			language = language
		)
		return misspelledRange.useContents { location } == NSNotFound.toULong()
	}

	actual suspend fun addToDictionary(word: String, scope: DictionaryScope) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		when (scope) {
			DictionaryScope.AppLocal -> userDict.add(trimmed)
			DictionaryScope.System -> UITextChecker.learnWord(trimmed)
		}
	}

	actual suspend fun removeFromDictionary(word: String, scope: DictionaryScope) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		when (scope) {
			DictionaryScope.AppLocal -> userDict.remove(trimmed)
			DictionaryScope.System -> UITextChecker.unlearnWord(trimmed)
		}
	}

	actual suspend fun ignoreWord(word: String) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		textChecker.ignoreWord(trimmed)
		// Also mirror to the local set so isWordCorrect/checkWord (which check
		// the spell-checker via range queries) see the ignore on the very next
		// call. UITextChecker.ignoreWord influences rangeOfMisspelledWord too,
		// but mirroring keeps semantics consistent if Apple changes that.
		userDict.ignore(trimmed)
	}

	actual fun userDictionary(): Set<String> = userDict.snapshot()

	actual override fun close() {
		// No resources to free for UITextChecker
	}
}
