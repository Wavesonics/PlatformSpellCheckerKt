package com.example.spellchecktest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.example.spellchecktest.ui.theme.SpellCheckTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SpellCheckTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SpellCheckContent(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SpellCheckContent(
    modifier: Modifier = Modifier,
    viewModel: SpellCheckViewModel = koinViewModel()
) {
    var textInput by remember { mutableStateOf("boooks") }
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Spell Check",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = textInput,
            onValueChange = {
                textInput = it
                viewModel.clearSuggestions()
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Text to check") },
            placeholder = { Text("Type text to spell check") }
        )

        Button(
            onClick = {
                viewModel.performSpellCheck(textInput)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Spell Check")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.suggestions) { suggestion ->
                Text(
                    text = suggestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SpellCheckContentPreview() {
    SpellCheckTestTheme {
        SpellCheckContent()
    }
}