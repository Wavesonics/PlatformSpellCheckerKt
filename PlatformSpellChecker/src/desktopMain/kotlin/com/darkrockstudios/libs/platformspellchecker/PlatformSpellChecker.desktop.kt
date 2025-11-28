package com.darkrockstudios.libs.platformspellchecker

/**
 * Desktop JVM implementation of PlatformSpellChecker.
 *
 * This is currently a stub implementation. A full implementation could use:
 * - Hunspell via JNA bindings
 * - LanguageTool
 * - A custom dictionary-based approach
 */
actual class PlatformSpellChecker {

    actual suspend fun performSpellCheck(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        // Stub implementation - returns placeholder message
        return listOf("Desktop spell checking not yet implemented. Text received: \"${text.take(50)}${if (text.length > 50) "..." else ""}\"")
    }

    actual suspend fun checkWord(word: String): List<String> {
        if (word.isBlank()) {
            return emptyList()
        }

        val trimmedWord = word.trim()
        if (trimmedWord.contains(" ")) {
            return listOf("Please provide a single word only")
        }

        // Stub implementation - returns placeholder message
        return listOf("Desktop spell checking not yet implemented. Word: '$trimmedWord'")
    }
}
