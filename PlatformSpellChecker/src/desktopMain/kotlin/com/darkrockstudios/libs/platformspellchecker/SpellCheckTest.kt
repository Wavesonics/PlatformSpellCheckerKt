package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.windows.WindowsSpellChecker
import kotlinx.coroutines.runBlocking

/**
 * Simple test main function to verify the Windows Spell Checker integration.
 * Run with: gradlew :PlatformSpellChecker:runSpellCheckTest
 */
fun main() = runBlocking {
    println("=== Windows Spell Checker Test ===")
    println()

    // Test 1: Check if language is supported
    println("1. Checking language support...")
    val isEnUsSupported = WindowsSpellChecker.isLanguageSupported("en-US")
    println("   en-US supported: $isEnUsSupported")

    if (!isEnUsSupported) {
        println("   ERROR: en-US is not supported. Windows Spell Checker may not be available.")
        return@runBlocking
    }

    // Test 2: Create spell checker
    println()
    println("2. Creating spell checker for en-US...")
    val spellChecker = WindowsSpellChecker.create("en-US")
    if (spellChecker == null) {
        println("   ERROR: Failed to create spell checker")
        return@runBlocking
    }
    println("   Success! Language: ${spellChecker.languageTag}")

    // Test 3: Get suggestions directly (skip IEnumSpellingError)
    println()
    println("3. Getting suggestions for 'helllo' (testing IEnumString)...")
    val suggestions = spellChecker.getSuggestions("helllo")
    println("   Suggestions: $suggestions")

    // Test 4: Check misspelled word using Check + IEnumSpellingError (now with fixed vtable indices)
    println()
    println("4. Checking misspelled word 'helllo' (with fixed vtable indices)...")
    val helllloCorrect = spellChecker.isWordCorrect("helllo")
    println("   'helllo' is correct: $helllloCorrect")

    // Test: Try getSuggestions for a clearly misspelled word
    println()
    println("4b. Getting suggestions for 'xyzzy123' (random string)...")
    val suggestions2 = spellChecker.getSuggestions("xyzzy123")
    println("   Suggestions: $suggestions2")

    println()
    println("4c. Getting suggestions for 'recieve' (common misspelling)...")
    val suggestions3 = spellChecker.getSuggestions("recieve")
    println("   Suggestions: $suggestions3")

    // Test 5: Get suggestions for misspelled word (duplicate removed)
    println()
    println("5. Test removed (duplicate of test 3)")

    // Test 6: Check text for errors (now with fixed vtable indices)
    println()
    println("6. Checking text: 'The quikc brown fox jumps over the layz dog'...")
    val errors = spellChecker.checkText("The quikc brown fox jumps over the layz dog")
    println("   Errors found: ${errors.size}")
    for (error in errors) {
        println("     - Position ${error.startIndex}, length ${error.length}, action ${error.correctiveAction}")
    }

    // Test 7: Test with PlatformSpellChecker
    println()
    println("7. Testing PlatformSpellChecker.checkWord('tset')...")
    val platformChecker = PlatformSpellChecker()
    val wordResults = platformChecker.checkWord("tset")
    println("   Results: $wordResults")

    // Test 8: Test sentence check
    println()
    println("8. Testing PlatformSpellChecker.performSpellCheck('This is a tset sentance')...")
    val sentenceResults = platformChecker.performSpellCheck("This is a tset sentance")
    println("   Results: $sentenceResults")

    // Cleanup
    spellChecker.close()
    println()
    println("=== Test Complete ===")
}
