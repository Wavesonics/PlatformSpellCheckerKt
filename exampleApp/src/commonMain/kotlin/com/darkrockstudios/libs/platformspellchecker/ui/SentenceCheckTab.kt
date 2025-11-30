package com.darkrockstudios.libs.platformspellchecker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libs.platformspellchecker.SpellCheckViewModel
import com.darkrockstudios.libs.platformspellchecker.SpellingCorrection

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
				corrections = uiState.corrections,
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
				corrections = uiState.corrections,
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
	corrections: List<SpellingCorrection>,
	modifier: Modifier = Modifier
) {
	LazyColumn(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		if (corrections.isEmpty()) {
			item {
				Text(
					text = "No spelling errors found",
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp)
				)
			}
		} else {
			items(corrections) { correction ->
				val header = "'${correction.misspelledWord}' at ${correction.startIndex} (${correction.length})"
				Text(
					text = if (correction.suggestions.isEmpty()) {
						"$header → no suggestions"
					} else {
						header
					},
					modifier = Modifier
						.fillMaxWidth()
						.padding(8.dp)
				)

				if (correction.suggestions.isNotEmpty()) {
					correction.suggestions.forEach { suggestion ->
						Text(
							text = "  • $suggestion",
							modifier = Modifier
								.fillMaxWidth()
								.padding(start = 16.dp, top = 2.dp, bottom = 2.dp)
						)
					}
				}
			}
		}
	}
}
