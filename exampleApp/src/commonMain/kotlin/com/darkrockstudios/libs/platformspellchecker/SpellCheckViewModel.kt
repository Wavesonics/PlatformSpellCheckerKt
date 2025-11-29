package com.darkrockstudios.libs.platformspellchecker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.darkrockstudios.libs.platformspellchecker.PlatformSpellChecker
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
    private val spellChecker: PlatformSpellChecker
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

            val suggestions = spellChecker.checkWord(_wordTabState.value.text)

            _wordTabState.value = _wordTabState.value.copy(
                suggestions = suggestions,
                isLoading = false
            )
        }
    }

    fun performSpellCheck() {
        viewModelScope.launch {
            _sentenceTabState.value = _sentenceTabState.value.copy(isLoading = true)

            val corrections = spellChecker.performSpellCheck(_sentenceTabState.value.text)

            // Convert corrections to display strings for the UI
            val displayStrings = if (corrections.isEmpty()) {
                listOf("No spelling errors found")
            } else {
                corrections.flatMap { correction ->
                    if (correction.suggestions.isEmpty()) {
                        listOf("'${correction.misspelledWord}' may be misspelled (no suggestions)")
                    } else {
                        correction.suggestions.map { suggestion ->
                            "'${correction.misspelledWord}' → '$suggestion'"
                        }
                    }
                }
            }

            _sentenceTabState.value = _sentenceTabState.value.copy(
                suggestions = displayStrings,
                isLoading = false
            )
        }
    }
}
