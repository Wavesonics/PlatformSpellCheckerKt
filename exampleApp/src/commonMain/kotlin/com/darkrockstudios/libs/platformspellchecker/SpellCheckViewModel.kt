package com.darkrockstudios.libs.platformspellchecker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class WordTabUiState(
	val text: String = "",
	val result: WordCheckResult? = null,
	val isLoading: Boolean = false
)

data class SentenceTabUiState(
	val text: String = "",
	val corrections: List<SpellingCorrection> = emptyList(),
	val isLoading: Boolean = false
)

class SpellCheckViewModel(
	private val spellChecker: PlatformSpellChecker
) : ViewModel() {
	private val _wordTabState = MutableStateFlow(WordTabUiState(text = "boooks"))
	private val _sentenceTabState = MutableStateFlow(SentenceTabUiState(text = "Ths is a tst sentence with erors"))

	val wordTabState: StateFlow<WordTabUiState> = _wordTabState.asStateFlow()
	val sentenceTabState: StateFlow<SentenceTabUiState> = _sentenceTabState.asStateFlow()

	fun updateWordText(text: String) {
		_wordTabState.value = _wordTabState.value.copy(
			text = text,
			result = null
		)
	}

	fun updateSentenceText(text: String) {
		_sentenceTabState.value = _sentenceTabState.value.copy(
			text = text,
			corrections = emptyList()
		)
	}

	fun checkWord() {
		viewModelScope.launch {
			_wordTabState.value = _wordTabState.value.copy(isLoading = true)

			val result = spellChecker.checkWord(_wordTabState.value.text)
			_wordTabState.value = _wordTabState.value.copy(
				result = result,
				isLoading = false
			)
		}
	}

	fun performSpellCheck() {
		viewModelScope.launch {
			_sentenceTabState.value = _sentenceTabState.value.copy(isLoading = true)

			val corrections = spellChecker.checkMultiword(_sentenceTabState.value.text)

			_sentenceTabState.value = _sentenceTabState.value.copy(
				corrections = corrections,
				isLoading = false
			)
		}
	}
}
