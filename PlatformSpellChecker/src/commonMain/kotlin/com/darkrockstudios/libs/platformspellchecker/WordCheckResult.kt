package com.darkrockstudios.libs.platformspellchecker

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface WordCheckResult {
	val word: String
}

data class CorrectWord(
	override val word: String
) : WordCheckResult

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