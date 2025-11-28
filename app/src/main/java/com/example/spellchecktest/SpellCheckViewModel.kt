package com.example.spellchecktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SpellCheckUiState(
    val suggestions: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class SpellCheckViewModel(
    private val repository: SpellCheckRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(SpellCheckUiState())

    val uiState: StateFlow<SpellCheckUiState> = _uiState.asStateFlow()

    fun performSpellCheck(text: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val suggestions = repository.performSpellCheck(text)

            _uiState.value = _uiState.value.copy(
                suggestions = suggestions,
                isLoading = false
            )
        }
    }

    fun checkWord(word: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val suggestions = repository.checkWord(word)

            _uiState.value = _uiState.value.copy(
                suggestions = suggestions,
                isLoading = false
            )
        }
    }

    fun clearSuggestions() {
        _uiState.value = _uiState.value.copy(suggestions = emptyList())
    }
}
