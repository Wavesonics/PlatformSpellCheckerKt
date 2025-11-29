package com.darkrockstudios.libs.platformspellchecker

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.UITextChecker
import io.github.aakira.napier.Napier

/**
 * iOS implementation of PlatformSpellChecker using UITextChecker.
 *
 * UITextChecker is part of UIKit and provides spell checking functionality
 * using the system's dictionaries and language settings.
 */
@OptIn(ExperimentalForeignApi::class)
actual class PlatformSpellChecker {

    private val textChecker = UITextChecker()
    private val language = "en_US" // Default to US English

    /**
     * Performs spell check on a sentence or multi-word text.
     * Returns a list of [SpellingCorrection] objects containing the misspelled words,
     * their positions in the original text, and suggested corrections.
     * Returns an empty list if no spelling errors are found.
     */
    actual suspend fun performSpellCheck(text: String): List<SpellingCorrection> {
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

            // Create SpellingCorrection object
            val correction = SpellingCorrection(
                misspelledWord = misspelledWord,
                startIndex = misspelledRange.useContents { location.toInt() },
                length = misspelledRange.useContents { length.toInt() },
                suggestions = suggestionList
            )
            results.add(correction)

            // Move to the next position after this misspelled word
            currentOffset = misspelledRange.useContents { location + length }
        }

        return results
    }

    /**
     * Checks a single word for spelling errors.
     * Returns spelling suggestions if the word is misspelled,
     * or "'word' is correctly spelled" if the word is correct.
     */
    actual suspend fun checkWord(word: String): List<String> {
        if (word.isBlank()) {
            return listOf("Please enter a word to check")
        }

        val range = NSMakeRange(0u, word.length.toULong())

        val misspelledRange = textChecker.rangeOfMisspelledWordInString(
            stringToCheck = word,
            range = range,
            startingAt = 0,
            wrap = false,
            language = language
        )

        // If NSNotFound, the word is correctly spelled
        if (misspelledRange.useContents { location } == NSNotFound.toULong()) {
            return listOf("'$word' is correctly spelled")
        }

        // Get suggestions for the misspelled word
        val suggestions = textChecker.guessesForWordRange(
            range = misspelledRange,
            inString = word,
            language = language
        ) as? List<*>

        return if (suggestions != null && suggestions.isNotEmpty()) {
            suggestions.mapNotNull { it as? String }
        } else {
            listOf("'$word' is misspelled but no suggestions are available")
        }
    }
}
