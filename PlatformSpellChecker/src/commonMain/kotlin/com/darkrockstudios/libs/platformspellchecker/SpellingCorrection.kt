package com.darkrockstudios.libs.platformspellchecker

/**
 * Represents a spelling correction for a misspelled word in a text.
 *
 * @property misspelledWord The word that was identified as misspelled.
 * @property startIndex The starting character index of the misspelled word in the original text.
 * @property length The length of the misspelled word.
 * @property suggestions A list of suggested corrections for the misspelled word.
 */
data class SpellingCorrection(
	val misspelledWord: String,
	val startIndex: Int,
	val length: Int,
	val suggestions: List<String>
)
