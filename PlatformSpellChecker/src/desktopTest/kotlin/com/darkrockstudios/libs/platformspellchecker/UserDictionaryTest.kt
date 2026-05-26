package com.darkrockstudios.libs.platformspellchecker

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for the internal [UserDictionary] helper. These run on every host
 * because they don't touch the native spell-checker backends.
 */
class UserDictionaryTest {

	@Test
	fun `added word is reported as known`() = runTest {
		val dict = UserDictionary()
		assertFalse(dict.isKnown("kotlin"))

		dict.add("kotlin")
		assertTrue(dict.isKnown("kotlin"))
	}

	@Test
	fun `lookup is case-insensitive and trims`() = runTest {
		val dict = UserDictionary()
		dict.add("Wavesonics")

		assertTrue(dict.isKnown("wavesonics"))
		assertTrue(dict.isKnown("WAVESONICS"))
		assertTrue(dict.isKnown("  Wavesonics  "))
	}

	@Test
	fun `remove deletes only that word`() = runTest {
		val dict = UserDictionary()
		dict.add("alpha")
		dict.add("beta")

		dict.remove("alpha")

		assertFalse(dict.isKnown("alpha"))
		assertTrue(dict.isKnown("beta"))
	}

	@Test
	fun `ignored words count as known but are not in snapshot`() = runTest {
		val dict = UserDictionary()
		dict.add("alpha")
		dict.ignore("beta")

		assertTrue(dict.isKnown("alpha"))
		assertTrue(dict.isKnown("beta"))

		assertEquals(setOf("alpha"), dict.snapshot())
	}

	@Test
	fun `blank inputs are ignored`() = runTest {
		val dict = UserDictionary()
		dict.add("")
		dict.add("   ")
		dict.ignore("\t\n")

		assertFalse(dict.isKnown(""))
		assertEquals(emptySet(), dict.snapshot())
	}

	@Test
	fun `replace swaps added words and clears ignores`() = runTest {
		val dict = UserDictionary()
		dict.add("alpha")
		dict.ignore("gamma")
		assertTrue(dict.isKnown("alpha"))
		assertTrue(dict.isKnown("gamma"))

		dict.replace(listOf("Beta", "delta"))

		assertFalse(dict.isKnown("alpha"))
		assertFalse(dict.isKnown("gamma"))
		assertTrue(dict.isKnown("beta"))
		assertTrue(dict.isKnown("DELTA"))
		assertEquals(setOf("beta", "delta"), dict.snapshot())
	}

	@Test
	fun `replace with empty collection clears the dictionary`() = runTest {
		val dict = UserDictionary()
		dict.add("alpha")
		dict.add("beta")

		dict.replace(emptyList())

		assertFalse(dict.isKnown("alpha"))
		assertEquals(emptySet(), dict.snapshot())
	}
}
