package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.native.CorrectiveAction
import com.darkrockstudios.libs.platformspellchecker.native.NativeSpellChecker
import com.darkrockstudios.libs.platformspellchecker.native.NativeSpellCheckerFactory
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Desktop JVM implementation of PlatformSpellChecker.
 *
 * Automatically detects the operating system and uses the appropriate native spell checker:
 * - Windows 8+: Windows Spell Checking API via JNA/COM
 * - macOS: NSSpellChecker via JNA/Objective-C runtime
 * - Linux: Hunspell/Enchant (to be implemented)
 */
actual class PlatformSpellChecker(
    private val locale: SpLocale? = null
) {

    private val spellChecker: NativeSpellChecker? by lazy {
        try {
            val checker = if (locale != null) {
                val tag = if (locale.country.isNullOrBlank()) locale.language else "${locale.language}-${locale.country}"
                NativeSpellCheckerFactory.create(tag)
            } else {
                NativeSpellCheckerFactory.createDefault()
            }
            checker
        } catch (e: Exception) {
            Napier.e("Failed to create spell checker: ${e.message}", e)
            null
        }
    }

    actual suspend fun performSpellCheck(text: String): List<SpellingCorrection> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext emptyList()
        }

        val checker = spellChecker
        if (checker == null) {
            return@withContext emptyList()
        }

        try {
            val errors = checker.checkText(text)

            errors.mapNotNull { error ->
                val misspelledWord = text.substring(error.startIndex, error.startIndex + error.length)

                when (error.correctiveAction) {
                    CorrectiveAction.GET_SUGGESTIONS -> {
                        val suggestions = checker.getSuggestions(misspelledWord).take(5)
                        SpellingCorrection(
                            misspelledWord = misspelledWord,
                            startIndex = error.startIndex,
                            length = error.length,
                            suggestions = suggestions
                        )
                    }
                    CorrectiveAction.REPLACE -> {
                        val replacement = error.replacement
                        if (replacement != null) {
                            SpellingCorrection(
                                misspelledWord = misspelledWord,
                                startIndex = error.startIndex,
                                length = error.length,
                                suggestions = listOf(replacement)
                            )
                        } else {
                            null
                        }
                    }
                    CorrectiveAction.DELETE -> {
                        SpellingCorrection(
                            misspelledWord = misspelledWord,
                            startIndex = error.startIndex,
                            length = error.length,
                            suggestions = emptyList()
                        )
                    }
                    CorrectiveAction.NONE -> null
                }
            }
        } catch (e: Exception) {
            Napier.e("Error checking spelling: ${e.message}", e)
            emptyList()
        }
    }

    actual suspend fun checkWord(word: String): List<String> = withContext(Dispatchers.IO) {
        if (word.isBlank()) {
            return@withContext emptyList()
        }

        val trimmedWord = word.trim()
        if (trimmedWord.contains(" ")) {
            return@withContext listOf("Please provide a single word only")
        }

        val checker = spellChecker
        if (checker == null) {
            val osName = NativeSpellCheckerFactory.getOSName()
            return@withContext listOf("Spell checking not available on $osName")
        }

        try {
            if (checker.isWordCorrect(trimmedWord)) {
                return@withContext listOf("'$trimmedWord' is correctly spelled")
            }

            val suggestions = checker.getSuggestions(trimmedWord)
            if (suggestions.isNotEmpty()) {
                suggestions.take(5)
            } else {
                listOf("'$trimmedWord' may be misspelled (no suggestions available)")
            }
        } catch (e: Exception) {
            listOf("Error checking word: ${e.message}")
        }
    }

    
}
