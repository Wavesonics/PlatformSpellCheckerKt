package com.darkrockstudios.libs.platformspellchecker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libs.platformspellchecker.SpellCheckViewModel

@Composable
fun SentenceCheckTab(
    isCompactHeight: Boolean,
    viewModel: SpellCheckViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.sentenceTabState.collectAsState()

    if (isCompactHeight) {
        // Row layout for compact height (landscape)
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputSection(
                text = uiState.text,
                isLoading = uiState.isLoading,
                onTextChange = { viewModel.updateSentenceText(it) },
                onCheckClick = { viewModel.performSpellCheck() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )

            SuggestionsSection(
                suggestions = uiState.suggestions,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        // Column layout for medium/expanded height (portrait)
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InputSection(
                text = uiState.text,
                isLoading = uiState.isLoading,
                onTextChange = { viewModel.updateSentenceText(it) },
                onCheckClick = { viewModel.performSpellCheck() },
                modifier = Modifier.fillMaxWidth()
            )

            SuggestionsSection(
                suggestions = uiState.suggestions,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun InputSection(
    text: String,
    isLoading: Boolean,
    onTextChange: (String) -> Unit,
    onCheckClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text to check") },
            placeholder = { Text("Type text to spell check") },
            minLines = 3
        )

        Button(
            onClick = onCheckClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...")
                }
            } else {
                Text("Check Sentence")
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(suggestions) { suggestion ->
            Text(
                text = suggestion,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}
