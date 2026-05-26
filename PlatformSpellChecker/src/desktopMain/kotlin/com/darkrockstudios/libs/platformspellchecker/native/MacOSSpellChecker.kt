package com.darkrockstudios.libs.platformspellchecker.native

import com.darkrockstudios.libs.platformspellchecker.macos.NSSpellCheckerWrapper
import io.github.aakira.napier.Napier
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * macOS implementation of NativeSpellChecker.
 *
 * Uses NSSpellChecker via JNA bindings to provide spell checking on macOS.
 */
class MacOSSpellChecker private constructor(
	override val languageTag: String,
	private val spellChecker: NSSpellCheckerWrapper,
	/** Per-call language for NSSpellChecker, or null for automatic detection. */
	private val effectiveLanguage: String?
) : NativeSpellChecker {

	// We keep a local set for fast lookups in isWordCorrect,
	// but the native spellChecker session handles ignores during checkText.
	private val ignoredWords = mutableSetOf<String>()

	override fun checkText(text: String): List<SpellingError> {
		if (text.isBlank()) return emptyList()

		val errors = mutableListOf<SpellingError>()

		try {
			// Use a substring window approach to work around JNA struct-return limitations.
			// Passing non-zero startingAt values can fail, so we slice the string instead
			// and always check from position 0.
			var remainingText = text
			var globalOffset = 0L

			val maxLoops = text.length * 2
			var loopCount = 0

			while (remainingText.isNotBlank() && loopCount < maxLoops) {
				loopCount++

				val errorRange = spellChecker.checkSpelling(remainingText, 0, effectiveLanguage)

				if (!errorRange.isFound()) {
					break
				}

				val errorStartGlobal = globalOffset + errorRange.location
				val errorLength = errorRange.length

				errors.add(
					SpellingError(
						startIndex = errorStartGlobal.toInt(),
						length = errorLength.toInt(),
						correctiveAction = CorrectiveAction.GET_SUGGESTIONS,
						replacement = null
					)
				)

				val nextStartInSubstring = errorRange.location + errorLength

				if (nextStartInSubstring >= remainingText.length) {
					break
				}

				remainingText = remainingText.substring(nextStartInSubstring.toInt())
				globalOffset += nextStartInSubstring
			}

			return errors
		} catch (e: Exception) {
			Napier.e("Error checking text: ${e.message}", e)
			return emptyList()
		}
	}

	override fun getSuggestions(word: String): List<String> {
		if (word.isBlank()) return emptyList()

		return try {
			spellChecker.getSuggestions(word.trim(), effectiveLanguage)
		} catch (e: Exception) {
			Napier.e("Error getting suggestions: ${e.message}", e)
			emptyList()
		}
	}

	override fun isWordCorrect(word: String): Boolean {
		if (word.isBlank()) return true

		val cleanWord = word.trim()

		if (ignoredWords.contains(cleanWord.lowercase())) {
			return true
		}

		return try {
			spellChecker.isWordCorrect(cleanWord, effectiveLanguage)
		} catch (e: Exception) {
			Napier.e("Error checking word: ${e.message}", e)
			true
		}
	}

	override fun addToDictionary(word: String) {
		if (word.isBlank()) return

		try {
			spellChecker.learnWord(word.trim())
		} catch (e: Exception) {
			Napier.e("Error adding word to dictionary: ${e.message}", e)
		}
	}

	override fun removeFromDictionary(word: String) {
		if (word.isBlank()) return

		try {
			spellChecker.unlearnWord(word.trim())
		} catch (e: Exception) {
			Napier.e("Error removing word from dictionary: ${e.message}", e)
		}
	}

	override fun ignoreWord(word: String) {
		if (word.isBlank()) return

		try {
			val cleanWord = word.trim()

			ignoredWords.add(cleanWord.lowercase())
			spellChecker.ignoreWord(cleanWord)
		} catch (e: Exception) {
			Napier.e("Error ignoring word: ${e.message}", e)
		}
	}

	override fun close() {
		try {
			spellChecker.close()
		} catch (e: Exception) {
			Napier.e("Error closing spell checker: ${e.message}", e)
		}
	}

	companion object {
		private val warmupStarted = AtomicBoolean(false)
		private val warmupComplete = CountDownLatch(1)

		/**
		 * First NSSpellChecker call per process can hit an IAC timeout
		 * (`NSSpellServer findMisspelledWordInString timed out`) and silently
		 * return NSNotFound. The first caller runs the warmup; concurrent
		 * callers block on the latch until it completes, so nobody races past
		 * with a cold server.
		 */
		private fun warmUpOnce(wrapper: NSSpellCheckerWrapper) {
			if (warmupStarted.compareAndSet(false, true)) {
				try {
					wrapper.getSuggestions("warmup", null)
					wrapper.checkSpelling("warmup", 0)
				} catch (e: Exception) {
					Napier.w("NSSpellChecker warmup failed (non-fatal): ${e.message}")
				} finally {
					warmupComplete.countDown()
				}
			} else {
				warmupComplete.await()
			}
		}

		/**
		 * Checks if a language is supported.
		 *
		 * @param languageTag BCP47 language tag (e.g., "en-US")
		 * @return true if supported
		 */
		fun isLanguageSupported(languageTag: String): Boolean {
			return try {
				NSSpellCheckerWrapper.isLanguageSupported(languageTag)
			} catch (e: Exception) {
				Napier.e("Error checking language support: ${e.message}", e)
				false
			}
		}

		/**
		 * Creates a spell checker for the specified language.
		 *
		 * @param languageTag BCP47 language tag (e.g., "en-US")
		 * @return MacOSSpellChecker instance, or null if creation failed
		 */
		fun create(languageTag: String): MacOSSpellChecker? {
			return try {
				val wrapper = NSSpellCheckerWrapper.shared() ?: return null
				warmUpOnce(wrapper)

				// Pass language per-call instead of via setLanguage: (which
				// Apple discourages and which mutates the shared singleton).
				// Fall back to null = automatic detection if no exact match.
				val available = NSSpellCheckerWrapper.availableLanguages()
				val effectiveLanguage = when {
					available.contains(languageTag) -> languageTag
					available.contains(languageTag.replace("-", "_")) -> languageTag.replace("-", "_")
					else -> null
				}

				MacOSSpellChecker(languageTag, wrapper, effectiveLanguage)
			} catch (e: Exception) {
				Napier.e("Error creating macOS spell checker: ${e.message}", e)
				null
			}
		}

		/**
		 * Creates a spell checker using the system's default language.
		 *
		 * @return MacOSSpellChecker instance, or null if creation failed
		 */
		fun createDefault(): MacOSSpellChecker? {
			return try {
				val wrapper = NSSpellCheckerWrapper.shared() ?: return null
				warmUpOnce(wrapper)

				val currentLanguage = wrapper.getLanguage()
				val languageTag = currentLanguage
					?: Locale.getDefault().toLanguageTag()

				MacOSSpellChecker(languageTag, wrapper, effectiveLanguage = null)
			} catch (e: Exception) {
				Napier.e("Error creating default macOS spell checker: ${e.message}", e)
				null
			}
		}

		/**
		 * Gets a list of available languages.
		 *
		 * @return List of language codes
		 */
		fun getAvailableLanguages(): List<String> {
			return try {
				NSSpellCheckerWrapper.availableLanguages()
			} catch (e: Exception) {
				Napier.e("Error getting available languages: ${e.message}", e)
				emptyList()
			}
		}
	}
}