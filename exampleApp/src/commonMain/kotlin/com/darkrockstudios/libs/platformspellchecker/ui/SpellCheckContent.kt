package com.darkrockstudios.libs.platformspellchecker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libs.platformspellchecker.SpellCheckViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SpellCheckContent(
	isCompactHeight: Boolean,
	modifier: Modifier = Modifier,
	viewModel: SpellCheckViewModel = koinViewModel()
) {
	var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
	val tabs = listOf("Word", "Sentence")

	Column(
		modifier = modifier
			.fillMaxSize()
	) {
		Text(
			"Spell Check",
			style = MaterialTheme.typography.headlineLarge,
			modifier = Modifier.padding(16.dp)
		)

		TabRow(selectedTabIndex = selectedTabIndex) {
			tabs.forEachIndexed { index, title ->
				Tab(
					selected = selectedTabIndex == index,
					onClick = {
						selectedTabIndex = index
					},
					text = { Text(title) }
				)
			}
		}

		when (selectedTabIndex) {
			0 -> WordCheckTab(
				isCompactHeight = isCompactHeight,
				viewModel = viewModel
			)

			1 -> SentenceCheckTab(
				isCompactHeight = isCompactHeight,
				viewModel = viewModel
			)
		}
	}
}
