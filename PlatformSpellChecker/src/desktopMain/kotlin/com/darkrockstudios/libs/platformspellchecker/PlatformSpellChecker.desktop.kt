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
actual class PlatformSpellChecker {

    private val spellChecker: NativeSpellChecker? by lazy {
        try {
            NativeSpellCheckerFactory.createDefault()
        } catch (e: Exception) {
            Napier.e("Failed to create spell checker: ${e.message}", e)
            null
        }
    }

    actual suspend fun performSpellCheck(text: String): List<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) {
            return@withContext emptyList()
        }

        val checker = spellChecker
        if (checker == null) {
            val osName = NativeSpellCheckerFactory.getOSName()
            return@withContext listOf("Spell checking not available on $osName")
        }

        try {
            val errors = checker.checkText(text)

            if (errors.isEmpty()) {
                return@withContext listOf("No spelling errors found")
            }

            val results = mutableListOf<String>()
            for (error in errors) {
                val misspelledWord = text.substring(error.startIndex, error.startIndex + error.length)

                when (error.correctiveAction) {
                    CorrectiveAction.GET_SUGGESTIONS -> {
                        val suggestions = checker.getSuggestions(misspelledWord)
                        if (suggestions.isNotEmpty()) {
                            for (suggestion in suggestions.take(5)) {
                                results.add("'$misspelledWord' → '$suggestion'")
                            }
                        } else {
                            results.add("'$misspelledWord' may be misspelled (no suggestions)")
                        }
                    }
                    CorrectiveAction.REPLACE -> {
                        val replacement = error.replacement
                        if (replacement != null) {
                            results.add("'$misspelledWord' → '$replacement' (auto-correct)")
                        }
                    }
                    CorrectiveAction.DELETE -> {
                        results.add("'$misspelledWord' should be deleted")
                    }
                    CorrectiveAction.NONE -> {
                        // No action needed
                    }
                }
            }

            if (results.isEmpty()) {
                listOf("No spelling errors found")
            } else {
                results
            }
        } catch (e: Exception) {
            listOf("Error checking spelling: ${e.message}")
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
