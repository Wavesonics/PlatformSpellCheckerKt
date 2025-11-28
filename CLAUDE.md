# SpellCheckTest - Android Spell Checker App

## Project Overview
An Android application to explore plugging into the system's spell checking API.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow

## Project Structure

### Main Components

#### UI Layer
- **MainActivity.kt** - Main activity hosting the Compose UI
- **SpellCheckContent** - Composable function containing:
  - Text input field (OutlinedTextField)
  - "Spell Check" button
  - LazyColumn for displaying spelling suggestions

#### ViewModel Layer
- **SpellCheckViewModel.kt** - Manages UI state and business logic
  - `SpellCheckUiState` - Data class holding suggestions list and loading state
  - `performSpellCheck(text: String)` - Performs spell check operation
  - `clearSuggestions()` - Clears suggestion list
  - Uses StateFlow for reactive state management

## Current Implementation

## How It Works
1. User enters text in the input field
2. User clicks "Spell Check" button
3. ViewModel's `performSpellCheck()` is called
4. ViewModel updates `uiState` with suggestions
5. UI automatically updates LazyColumn with new suggestions

## Development Notes
- Uses version catalog for dependency management (gradle/libs.versions.toml)
- Follows Material 3 design guidelines
- State hoisting pattern for compose components
