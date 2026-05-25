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
) : AutoCloseable {

	private val userDict = UserDictionary()

	private val spellChecker: NativeSpellChecker? by lazy {
		try {
			val checker = if (locale != null) {
				val tag =
					if (locale.country.isNullOrBlank()) locale.language else "${locale.language}-${locale.country}"
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

	actual suspend fun checkMultiword(text: String): List<SpellingCorrection> = withContext(Dispatchers.IO) {
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
				if (userDict.isKnown(misspelledWord)) return@mapNotNull null

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

	actual suspend fun checkWord(word: String, maxSuggestions: Int): WordCheckResult = withContext(Dispatchers.IO) {
		val trimmed = word.trim()
		if (trimmed.isEmpty() || trimmed.contains(" ")) return@withContext CorrectWord(trimmed)
		if (userDict.isKnown(trimmed)) return@withContext CorrectWord(trimmed)

		val checker = spellChecker ?: return@withContext CorrectWord(trimmed)

		val max = if (maxSuggestions <= 0) 5 else maxSuggestions
		return@withContext try {
			if (checker.isWordCorrect(trimmed)) {
				CorrectWord(trimmed)
			} else {
				val suggestions = checker.getSuggestions(trimmed).take(max)
				MisspelledWord(trimmed, suggestions)
			}
		} catch (e: Exception) {
			Napier.e("Error checking word: ${e.message}", e)
			CorrectWord(trimmed)
		}
	}

	actual suspend fun isWordCorrect(word: String): Boolean = withContext(Dispatchers.IO) {
		val trimmed = word.trim()
		if (trimmed.isEmpty() || trimmed.contains(" ")) return@withContext false
		if (userDict.isKnown(trimmed)) return@withContext true

		val checker = spellChecker ?: return@withContext false
		return@withContext try {
			checker.isWordCorrect(trimmed)
		} catch (e: Exception) {
			Napier.e("Error checking word correctness: ${e.message}", e)
			false
		}
	}

	actual suspend fun addToDictionary(word: String, scope: DictionaryScope) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		when (scope) {
			DictionaryScope.AppLocal -> userDict.add(trimmed)
			DictionaryScope.System -> withContext(Dispatchers.IO) {
				val checker = spellChecker
				if (checker == null) {
					Napier.w("addToDictionary(System): no native spell checker available; falling back to AppLocal")
					userDict.add(trimmed)
					return@withContext
				}
				try {
					checker.addToDictionary(trimmed)
				} catch (e: Exception) {
					Napier.e("Error adding '$trimmed' to system dictionary: ${e.message}", e)
				}
			}
		}
	}

	actual suspend fun removeFromDictionary(word: String, scope: DictionaryScope) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		when (scope) {
			DictionaryScope.AppLocal -> userDict.remove(trimmed)
			DictionaryScope.System -> withContext(Dispatchers.IO) {
				val checker = spellChecker
				if (checker == null) {
					Napier.w("removeFromDictionary(System): no native spell checker available; falling back to AppLocal")
					userDict.remove(trimmed)
					return@withContext
				}
				try {
					checker.removeFromDictionary(trimmed)
				} catch (e: Exception) {
					Napier.e("Error removing '$trimmed' from system dictionary: ${e.message}", e)
				}
			}
		}
	}

	actual suspend fun ignoreWord(word: String) {
		val trimmed = word.trim()
		if (trimmed.isEmpty()) return
		withContext(Dispatchers.IO) {
			try {
				spellChecker?.ignoreWord(trimmed)
			} catch (e: Exception) {
				Napier.e("Error ignoring '$trimmed': ${e.message}", e)
			}
		}
		userDict.ignore(trimmed)
	}

	actual suspend fun setUserDictionary(words: Collection<String>) {
		userDict.replace(words)
	}

	actual fun userDictionary(): Set<String> = userDict.snapshot()

	actual override fun close() {
		runCatching { spellChecker?.close() }
	}
}
