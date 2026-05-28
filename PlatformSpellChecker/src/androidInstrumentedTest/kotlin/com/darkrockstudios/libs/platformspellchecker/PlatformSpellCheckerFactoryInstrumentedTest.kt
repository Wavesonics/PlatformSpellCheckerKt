package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real device, so the set of installed spell-checkers is unknown.
 * These assertions are therefore invariants that must hold on ANY device,
 * not checks for specific locales.
 */
@RunWith(AndroidJUnit4::class)
class PlatformSpellCheckerFactoryInstrumentedTest {

	private lateinit var context: Context
	private lateinit var factory: PlatformSpellCheckerFactory

	@Before
	fun setup() {
		context = InstrumentationRegistry.getInstrumentation().targetContext
		factory = PlatformSpellCheckerFactory(context)
	}

	@Test
	fun blankLanguageIsNeverSupported() {
		assertFalse(factory.hasLanguage(SpLocale("")))
	}

	@Test
	fun everyAdvertisedLocaleReportsAsSupported() {
		// availableLocales() and hasLanguage() must agree: anything we advertise
		// as available must also pass the support check.
		for (locale in factory.availableLocales()) {
			assertTrue(
				"availableLocales() returned $locale but hasLanguage() rejected it",
				factory.hasLanguage(locale)
			)
		}
	}

	@Test
	fun createSpellCheckerSucceedsForEveryAdvertisedLocale() = runBlocking {
		for (locale in factory.availableLocales()) {
			factory.createSpellChecker(locale).close()
		}
	}

	@Test
	fun createSpellCheckerRejectsLocalesHasLanguageDenies() = runBlocking {
		// Probe a locale the factory says it can't service; createSpellChecker
		// must refuse it rather than build a doomed checker.
		val probe = SpLocale("zz", "ZZ")
		if (!factory.hasLanguage(probe)) {
			assertThrows(IllegalArgumentException::class.java) {
				runBlocking { factory.createSpellChecker(probe) }
			}
		}
		Unit
	}

	@Test
	fun availableLocalesIsEmptyWhenUnavailable() {
		// If spell checking is disabled/absent, we must not advertise locales.
		if (!factory.isAvailable()) {
			assertTrue(factory.availableLocales().isEmpty())
		}
	}
}
