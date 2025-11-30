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
import com.darkrockstudios.libs.platformspellchecker.CorrectWord
import com.darkrockstudios.libs.platformspellchecker.MisspelledWord
import com.darkrockstudios.libs.platformspellchecker.SpellCheckViewModel
import com.darkrockstudios.libs.platformspellchecker.WordCheckResult

@Composable
fun WordCheckTab(
	isCompactHeight: Boolean,
	viewModel: SpellCheckViewModel,
	modifier: Modifier = Modifier
) {
	val uiState by viewModel.wordTabState.collectAsState()

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
				onTextChange = { viewModel.updateWordText(it.filter { char -> !char.isWhitespace() }) },
				onCheckClick = { viewModel.checkWord() },
				modifier = Modifier
					.weight(1f)
					.fillMaxHeight()
			)

			SuggestionsSection(
				result = uiState.result,
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
				onTextChange = { viewModel.updateWordText(it.filter { char -> !char.isWhitespace() }) },
				onCheckClick = { viewModel.checkWord() },
				modifier = Modifier.fillMaxWidth()
			)

			SuggestionsSection(
				result = uiState.result,
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
			label = { Text("Word to check") },
			placeholder = { Text("Type a single word") }
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
				Text("Check Word")
			}
		}
	}
}

@Composable
private fun SuggestionsSection(
	result: WordCheckResult?,
	modifier: Modifier = Modifier
) {
	LazyColumn(
		modifier = modifier,
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		when (result) {
			null -> {
				// Show nothing by default
			}

			is CorrectWord -> {
				item {
					Text(
						text = "'${result.word}' is spelled correctly",
						modifier = Modifier
							.fillMaxWidth()
							.padding(8.dp)
					)
				}
			}

			is MisspelledWord -> {
				if (result.suggestions.isEmpty()) {
					item {
						Text(
							text = "'${result.word}' may be misspelled (no suggestions)",
							modifier = Modifier
								.fillMaxWidth()
								.padding(8.dp)
						)
					}
				} else {
					items(result.suggestions) { suggestion ->
						Text(
							text = "'${result.word}' → '$suggestion'",
							modifier = Modifier
								.fillMaxWidth()
								.padding(8.dp)
						)
					}
				}
			}
		}
	}
}
