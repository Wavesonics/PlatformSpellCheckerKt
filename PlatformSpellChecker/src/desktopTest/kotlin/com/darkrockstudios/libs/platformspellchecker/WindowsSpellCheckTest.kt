package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.windows.WindowsSpellChecker
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * JUnit tests for the Windows Spell Checker integration.
 * Run with: gradlew :PlatformSpellChecker:desktopTest
 */
class WindowsSpellCheckTest {

    private var spellChecker: WindowsSpellChecker? = null

    @Before
    fun setup() {
        // Skip tests if en-US is not supported (e.g., running on non-Windows or without spell checker)
        val isSupported = WindowsSpellChecker.isLanguageSupported("en-US")
        assumeTrue("en-US spell checker not available", isSupported)

        spellChecker = WindowsSpellChecker.create("en-US")
        assumeTrue("Failed to create spell checker", spellChecker != null)
    }

    @Test
    fun `language tag is correct`() {
        assertEquals("en-US", spellChecker!!.languageTag)
    }

    @Test
    fun `correctly spelled word returns true`() {
        assertTrue(spellChecker!!.isWordCorrect("hello"))
    }

    @Test
    fun `misspelled word returns false`() {
        // Use a clearly misspelled word - "xyzabc" is not a real word
        kotlin.test.assertFalse(spellChecker!!.isWordCorrect("xyzabc"), "Expected 'xyzabc' to be marked as misspelled")
    }

    @Test
    fun `suggestions for misspelled word are not empty`() {
        val suggestions = spellChecker!!.getSuggestions("helllo")
        assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'helllo'")
        assertTrue(suggestions.contains("hello"), "Expected 'hello' in suggestions")
    }

    @Test
    fun `suggestions for common misspelling`() {
        val suggestions = spellChecker!!.getSuggestions("recieve")
        assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'recieve'")
        assertTrue(suggestions.contains("receive"), "Expected 'receive' in suggestions")
    }

    @Test
    fun `checkText finds errors in misspelled sentence`() {
        val errors = spellChecker!!.checkText("The quikc brown fox jumps over the layz dog")
        assertTrue(errors.isNotEmpty(), "Expected errors in text with misspellings")
        assertTrue(errors.size >= 2, "Expected at least 2 errors (quikc, layz)")
    }

    @Test
    fun `checkText returns empty for correct sentence`() {
        val errors = spellChecker!!.checkText("The quick brown fox jumps over the lazy dog")
        assertTrue(errors.isEmpty(), "Expected no errors in correctly spelled text")
    }

    @Test
    fun `PlatformSpellChecker checkWord returns suggestions for misspelled word`() = runTest {
        val platformChecker = PlatformSpellChecker()
        val results = platformChecker.checkWord("tset")
        assertTrue(results.isNotEmpty(), "Expected suggestions for 'tset'")
    }

    @Test
    fun `PlatformSpellChecker checkWord returns correct message for correct word`() = runTest {
        val platformChecker = PlatformSpellChecker()
        val results = platformChecker.checkWord("test")
        assertTrue(results.size == 1, "Expected single result for correctly spelled word")
        assertTrue(results[0].contains("correctly spelled"), "Expected 'correctly spelled' message")
    }

    @Test
    fun `PlatformSpellChecker performSpellCheck finds misspellings`() = runTest {
        val platformChecker = PlatformSpellChecker()
        val corrections = platformChecker.performSpellCheck("This is a tset sentance")
        assertTrue(corrections.isNotEmpty(), "Expected misspellings to be found")
        // Results should contain SpellingCorrection objects with suggestions
        assertTrue(corrections.any { it.suggestions.isNotEmpty() }, "Expected corrections with suggestions")
    }

    @Test
    fun `PlatformSpellChecker performSpellCheck returns empty for correct text`() = runTest {
        val platformChecker = PlatformSpellChecker()
        val corrections = platformChecker.performSpellCheck("This is a test sentence")
        assertTrue(corrections.isEmpty(), "Expected no corrections for correct text")
    }
}
