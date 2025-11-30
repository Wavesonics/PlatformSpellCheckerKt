package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.linux.HunspellLibrary
import com.darkrockstudios.libs.platformspellchecker.linux.HunspellWrapper
import com.darkrockstudios.libs.platformspellchecker.native.LinuxSpellChecker
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * JUnit tests for the Linux Spell Checker (Hunspell) integration.
 * Run with: gradlew :PlatformSpellChecker:desktopTest
 *
 * These tests require:
 * - libhunspell to be installed (e.g., `sudo apt install libhunspell-dev`)
 * - An English dictionary (e.g., `sudo apt install hunspell-en-us`)
 */
class LinuxSpellCheckTest {

	private var spellChecker: LinuxSpellChecker? = null

	@Before
	fun setup() {
		// Skip tests if not on Linux or Hunspell is not available
		val isLinux = System.getProperty("os.name").lowercase().contains("linux")
		assumeTrue("Not running on Linux", isLinux)

		val isAvailable = HunspellLibrary.isAvailable()
		assumeTrue("Hunspell library not available", isAvailable)

		// Skip tests if en-US is not supported
		val isSupported = LinuxSpellChecker.isLanguageSupported("en-US")
		assumeTrue("en-US dictionary not available", isSupported)

		spellChecker = LinuxSpellChecker.create("en-US")
		assumeTrue("Failed to create spell checker", spellChecker != null)
	}

	@Test
	fun `hunspell library is available`() {
		assertTrue(HunspellLibrary.isAvailable(), "Hunspell library should be available")
	}

	@Test
	fun `dictionary files can be found`() {
		val dictFiles = HunspellWrapper.findDictionaryFiles("en-US")
		assertTrue(dictFiles != null, "Should find en-US dictionary files")
		dictFiles?.let { (aff, dic) ->
			assertTrue(aff.endsWith(".aff"), "Should find .aff file")
			assertTrue(dic.endsWith(".dic"), "Should find .dic file")
		}
	}

	@Test
	fun `available languages are listed`() {
		val languages = HunspellWrapper.getAvailableLanguages()
		assertTrue(languages.isNotEmpty(), "Should find at least one language")
		println("Available languages: $languages")
	}

	@Test
	fun `language tag is correct`() {
		assertEquals("en-US", spellChecker!!.languageTag)
	}

	@Test
	fun `correctly spelled word returns true`() {
		assertTrue(spellChecker!!.isWordCorrect("hello"))
		assertTrue(spellChecker!!.isWordCorrect("world"))
		assertTrue(spellChecker!!.isWordCorrect("computer"))
	}

	@Test
	fun `misspelled word returns false`() {
		assertFalse(spellChecker!!.isWordCorrect("helllo"), "Expected 'helllo' to be misspelled")
		assertFalse(spellChecker!!.isWordCorrect("xyzabc"), "Expected 'xyzabc' to be misspelled")
	}

	@Test
	fun `suggestions for misspelled word are not empty`() {
		val suggestions = spellChecker!!.getSuggestions("helllo")
		assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'helllo'")
		println("Suggestions for 'helllo': $suggestions")
		assertTrue(suggestions.any { it.lowercase() == "hello" }, "Expected 'hello' in suggestions")
	}

	@Test
	fun `suggestions for common misspelling`() {
		val suggestions = spellChecker!!.getSuggestions("recieve")
		assertTrue(suggestions.isNotEmpty(), "Expected suggestions for 'recieve'")
		println("Suggestions for 'recieve': $suggestions")
		assertTrue(suggestions.any { it.lowercase() == "receive" }, "Expected 'receive' in suggestions")
	}

	@Test
	fun `checkText finds errors in misspelled sentence`() {
		val errors = spellChecker!!.checkText("The quikc brown fox jumps over the layz dog")
		assertTrue(errors.isNotEmpty(), "Expected errors in text with misspellings")
		assertTrue(errors.size >= 2, "Expected at least 2 errors (quikc, layz)")
		println("Found ${errors.size} errors: ${errors.map { e -> "start=${e.startIndex}, len=${e.length}" }}")
	}

	@Test
	fun `checkText returns empty for correct sentence`() {
		val errors = spellChecker!!.checkText("The quick brown fox jumps over the lazy dog")
		assertTrue(errors.isEmpty(), "Expected no errors in correctly spelled text")
	}

	@Test
	fun `ignore word works`() {
		val misspelled = "xyztest"
		assertFalse(spellChecker!!.isWordCorrect(misspelled), "Word should be misspelled initially")

		spellChecker!!.ignoreWord(misspelled)
		assertTrue(spellChecker!!.isWordCorrect(misspelled), "Word should be correct after ignoring")
	}

	@Test
	fun `PlatformSpellChecker checkWord returns suggestions for misspelled word`() = runTest {
		assumeTrue("Not on Linux", System.getProperty("os.name").lowercase().contains("linux"))

		val platformChecker = PlatformSpellChecker()
		val result = platformChecker.checkWord("tset")
		when (result) {
			is CorrectWord -> kotlin.test.fail("Expected misspelled result for 'tset'")
			is MisspelledWord -> {
				assertTrue(result.suggestions.isNotEmpty(), "Expected suggestions for 'tset'")
				println("PlatformSpellChecker suggestions for 'tset': ${result.suggestions}")
			}
		}
	}

	@Test
	fun `PlatformSpellChecker checkWord returns empty list for correct word`() = runTest {
		assumeTrue("Not on Linux", System.getProperty("os.name").lowercase().contains("linux"))

		val platformChecker = PlatformSpellChecker()
		val result = platformChecker.checkWord("test")
		assertTrue(result is CorrectWord, "Expected CorrectWord for correctly spelled word")
	}

	@Test
	fun `PlatformSpellChecker performSpellCheck finds misspellings`() = runTest {
		assumeTrue("Not on Linux", System.getProperty("os.name").lowercase().contains("linux"))

		val platformChecker = PlatformSpellChecker()
		val corrections = platformChecker.checkMultiword("This is a tset sentance")
		assertTrue(corrections.isNotEmpty(), "Expected misspellings to be found")
		println("PlatformSpellChecker corrections: $corrections")
		// Results should contain SpellingCorrection objects with suggestions
		assertTrue(corrections.any { it.suggestions.isNotEmpty() }, "Expected corrections with suggestions")
	}

	@Test
	fun `PlatformSpellChecker performSpellCheck returns empty for correct text`() = runTest {
		assumeTrue("Not on Linux", System.getProperty("os.name").lowercase().contains("linux"))

		val platformChecker = PlatformSpellChecker()
		val corrections = platformChecker.checkMultiword("This is a test sentence")
		assertTrue(corrections.isEmpty(), "Expected no corrections for correct text")
	}
}
