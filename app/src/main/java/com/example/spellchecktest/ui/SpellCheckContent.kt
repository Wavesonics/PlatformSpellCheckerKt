package com.example.spellchecktest.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.spellchecktest.SpellCheckViewModel
import com.example.spellchecktest.ui.theme.SpellCheckTestTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SpellCheckContent(
    windowSizeClass: WindowSizeClass,
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
                windowHeightSizeClass = windowSizeClass.heightSizeClass,
                viewModel = viewModel
            )
            1 -> SentenceCheckTab(
                windowHeightSizeClass = windowSizeClass.heightSizeClass,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Preview(showBackground = true)
@Composable
fun SpellCheckContentPreview() {
    SpellCheckTestTheme {
        SpellCheckContent(WindowSizeClass.calculateFromSize(
            DpSize(1024.dp, 2046.dp)
        ))
    }
}