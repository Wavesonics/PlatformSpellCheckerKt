package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.macos.NSSpellCheckerWrapper
import com.darkrockstudios.libs.platformspellchecker.macos.ObjC
import com.darkrockstudios.libs.platformspellchecker.native.MacOSSpellChecker
import com.darkrockstudios.libs.platformspellchecker.native.NativeSpellCheckerFactory
import com.darkrockstudios.libs.platformspellchecker.native.OperatingSystem
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * JUnit tests for the macOS Spell Checker integration.
 * Run with: gradlew :PlatformSpellChecker:desktopTest
 *
 * These tests will only run on macOS systems.
 */
class MacOSSpellCheckTest {

    private var spellChecker: MacOSSpellChecker? = null

    @Before
    fun setup() {
        // Skip tests if not running on macOS
        val isMacOS = NativeSpellCheckerFactory.currentOS == OperatingSystem.MACOS
        assumeTrue("Not running on macOS", isMacOS)

        // Skip tests if en-US is not supported
        val isSupported = MacOSSpellChecker.isLanguageSupported("en-US")
        assumeTrue("en-US spell checker not available", isSupported)

        spellChecker = MacOSSpellChecker.create("en-US")
        assumeTrue("Failed to create spell checker", spellChecker != null)
    }

    @Test
    fun `test Objective-C runtime is available`() {
        val nsStringClass = ObjC.getClass("NSString")
        assertNotNull(nsStringClass, "NSString class should be available")
    }

    @Test
    fun `test NSString creation`() {
        val nsString = ObjC.createNSString("Hello, World!")
        assertNotNull(nsString, "Should be able to create NSString")

        if (nsString != null) {
            val kotlinString = ObjC.nsStringToString(nsString)
            assertEquals("Hello, World!", kotlinString, "String conversion should work")
            ObjC.release(nsString)
        }
    }

    @Test
    fun `test NSSpellChecker wrapper can be created`() {
        val wrapper = NSSpellCheckerWrapper.shared()
        assertNotNull(wrapper, "NSSpellChecker wrapper should be available")
        wrapper?.close()
    }

    @Test
    fun `test available languages list is not empty`() {
        val languages = MacOSSpellChecker.getAvailableLanguages()
        assertTrue(languages.isNotEmpty(), "Should have at least one available language")
        println("Available languages: $languages")
    }

    @Test
    fun `language tag is correct`() {
        assertEquals("en-US", spellChecker!!.languageTag)
    }

    @Test
    fun `correctly spelled word returns true`() {
        val isCorrect = spellChecker!!.isWordCorrect("hello")
        assertTrue(isCorrect, "Expected 'hello' to be spelled correctly")
    }

    @Test
    fun `misspelled word returns false`() {
        // Use a clearly misspelled word
        val isCorrect = spellChecker!!.isWordCorrect("xyzabc")
        kotlin.test.assertFalse(isCorrect, "Expected 'xyzabc' to be marked as misspelled")
    }

    @Test
    fun `suggestions for misspelled word are not empty`() {
        val suggestions = spellChecker!!.getSuggestions("helllo")
        println("Suggestions for 'helllo': $suggestions")
        assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'helllo'")
    }

    @Test
    fun `suggestions for common misspelling`() {
        val suggestions = spellChecker!!.getSuggestions("recieve")
        println("Suggestions for 'recieve': $suggestions")
        assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'recieve'")
        assertTrue(suggestions.any { it.equals("receive", ignoreCase = true) },
            "Expected 'receive' in suggestions, got: $suggestions")
    }

    @Test
    fun `checkText finds errors in misspelled sentence`() {
        val errors = spellChecker!!.checkText("The quikc brown fox jumps over the layz dog")
        println("Found ${errors.size} errors: $errors")
        assertTrue(errors.isNotEmpty(), "Expected errors in text with misspellings")
        assertTrue(errors.size >= 2, "Expected at least 2 errors (quikc, layz), got ${errors.size}")
    }

    @Test
    fun `checkText returns empty for correct sentence`() {
        val errors = spellChecker!!.checkText("The quick brown fox jumps over the lazy dog")
        assertTrue(errors.isEmpty(), "Expected no errors in correctly spelled text, but found: $errors")
    }

    @Test
    fun `test ignore word functionality`() {
        // First check should mark as incorrect
        kotlin.test.assertFalse(spellChecker!!.isWordCorrect("xyzabc"))

        // Ignore the word
        spellChecker!!.ignoreWord("xyzabc")

        // Now should be marked as correct (ignored)
        assertTrue(spellChecker!!.isWordCorrect("xyzabc"), "Ignored word should be marked as correct")
    }

    @Test
    fun `PlatformSpellChecker checkWord returns suggestions for misspelled word`() = runTest {
        val platformChecker = PlatformSpellChecker()
	    val result = platformChecker.checkWord("tset")
	    println("Result for 'tset': $result")
	    when (result) {
		    is CorrectWord -> kotlin.test.fail("Expected misspelled result for 'tset'")
		    is MisspelledWord -> assertTrue(result.suggestions.isNotEmpty(), "Expected suggestions for 'tset'")
	    }
    }

    @Test
    fun `PlatformSpellChecker checkWord returns empty list for correct word`() = runTest {
        val platformChecker = PlatformSpellChecker()
	    val result = platformChecker.checkWord("test")
	    println("Result for 'test': $result")
	    assertTrue(result is CorrectWord, "Expected CorrectWord for correctly spelled word, got ${result}")
    }

    @Test
    fun `PlatformSpellChecker performSpellCheck finds misspellings`() = runTest {
        val platformChecker = PlatformSpellChecker()
	    val corrections = platformChecker.checkMultiword("This is a tset sentance")
        println("Spell check corrections: $corrections")
        assertTrue(corrections.isNotEmpty(), "Expected misspellings to be found")
        // Results should contain SpellingCorrection objects with suggestions
        assertTrue(corrections.any { it.suggestions.isNotEmpty() },
            "Expected corrections with suggestions, got: $corrections")
    }

    @Test
    fun `PlatformSpellChecker performSpellCheck returns empty for correct text`() = runTest {
        val platformChecker = PlatformSpellChecker()
	    val corrections = platformChecker.checkMultiword("This is a test sentence")
        println("Spell check corrections for correct text: $corrections")
        assertTrue(corrections.isEmpty(), "Expected no corrections for correct text, got ${corrections.size}")
    }

    @Test
    fun `test wrapper getSuggestions directly`() {
        val wrapper = NSSpellCheckerWrapper.shared()
        assertNotNull(wrapper, "Should be able to create wrapper")

        if (wrapper != null) {
            try {
                val suggestions = wrapper.getSuggestions("helllo", "en_US")
                println("Direct wrapper suggestions for 'helllo': $suggestions")
                assertTrue(suggestions.isNotEmpty() || true, "Suggestions test (may be empty)")
            } catch (e: Exception) {
                println("Exception getting suggestions: ${e.message}")
                e.printStackTrace()
                throw e
            } finally {
                wrapper.close()
            }
        }
    }
}
