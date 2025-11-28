package com.example.spellchecktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TabUiState(
    val text: String = "",
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class SpellCheckViewModel(
    private val repository: SpellCheckRepository
) : ViewModel() {
    private val _wordTabState = MutableStateFlow(TabUiState(text = "boooks"))
    private val _sentenceTabState = MutableStateFlow(TabUiState(text = "Ths is a tst sentence with erors"))

    val wordTabState: StateFlow<TabUiState> = _wordTabState.asStateFlow()
    val sentenceTabState: StateFlow<TabUiState> = _sentenceTabState.asStateFlow()

    fun updateWordText(text: String) {
        _wordTabState.value = _wordTabState.value.copy(
            text = text,
            suggestions = emptyList()
        )
    }

    fun updateSentenceText(text: String) {
        _sentenceTabState.value = _sentenceTabState.value.copy(
            text = text,
            suggestions = emptyList()
        )
    }

    fun checkWord() {
        viewModelScope.launch {
            _wordTabState.value = _wordTabState.value.copy(isLoading = true)

            val suggestions = repository.checkWord(_wordTabState.value.text)

            _wordTabState.value = _wordTabState.value.copy(
                suggestions = suggestions,
                isLoading = false
            )
        }
    }

    fun performSpellCheck() {
        viewModelScope.launch {
            _sentenceTabState.value = _sentenceTabState.value.copy(isLoading = true)

            val suggestions = repository.performSpellCheck(_sentenceTabState.value.text)

            _sentenceTabState.value = _sentenceTabState.value.copy(
                suggestions = suggestions,
                isLoading = false
            )
        }
    }
}
