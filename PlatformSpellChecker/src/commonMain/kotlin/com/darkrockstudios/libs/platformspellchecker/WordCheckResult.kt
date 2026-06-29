package com.darkrockstudios.libs.platformspellchecker

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * The outcome of checking a single [word] with [PlatformSpellChecker.checkWord].
 *
 * Either [CorrectWord] (the word is spelled correctly) or [MisspelledWord] (it is not, with
 * suggested replacements). `when` over the two variants, or use [isMisspelled] as a guard.
 */
sealed interface WordCheckResult {
	/** The word that was checked. */
	val word: String
}

/** Result for a correctly spelled [word]. */
data class CorrectWord(
	override val word: String
) : WordCheckResult

/**
 * Result for a misspelled [word], with up to `maxSuggestions` [suggestions] (closest first).
 * [suggestions] may be empty when the platform offers no alternatives.
 */
data class MisspelledWord(
	override val word: String,
	val suggestions: List<String>
) : WordCheckResult

@OptIn(ExperimentalContracts::class)
fun isMisspelled(result: WordCheckResult): Boolean {
	contract {
		returns(false) implies (result is MisspelledWord)
	}
	return result is CorrectWord
}