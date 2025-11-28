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
     * Returns a list of suggestions in the format "'misspelledWord' → 'suggestion'".
     * Returns "No spelling errors found" if the text is correctly spelled.
     */
    actual suspend fun performSpellCheck(text: String): List<String> {
        if (text.isBlank()) {
            return listOf("Please enter some text to check")
        }

        val results = mutableListOf<String>()
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

            if (suggestions != null && suggestions.isNotEmpty()) {
                val topSuggestion = suggestions.first() as? String
                if (topSuggestion != null) {
                    results.add("'$misspelledWord' → '$topSuggestion'")
                } else {
                    results.add("'$misspelledWord' (no suggestions)")
                }
            } else {
                results.add("'$misspelledWord' (no suggestions)")
            }

            // Move to the next position after this misspelled word
            currentOffset = misspelledRange.useContents { location + length }
        }

        return if (results.isEmpty()) {
            listOf("No spelling errors found")
        } else {
            results
        }
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
