# PlatformSpellChecker - Kotlin Multiplatform Spell Checker

## Project Overview
A Kotlin Multiplatform project demonstrating platform-specific spell checking functionality with a shared Compose Multiplatform UI.

## Tech Stack
- **Language**: Kotlin
- **Build System**: Gradle with Kotlin DSL
- **UI Framework**: Compose Multiplatform
- **Architecture**: MVVM (Model-View-ViewModel)
- **State Management**: StateFlow
- **Dependency Injection**: Koin (Multiplatform)
- **Platforms**: Android, Desktop (JVM)

## Project Structure

```
PlatformSpellChecker/
├── PlatformSpellChecker/          # KMP Library Module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/            # Expect declarations
│       ├── androidMain/           # Android actual implementation
│       └── desktopMain/           # Desktop actual implementation (stubbed)
│
├── exampleApp/                    # Compose Multiplatform App Module
│   ├── build.gradle.kts
│   └── src/
│       ├── commonMain/            # Shared UI and ViewModel
│       ├── androidMain/           # Android app entry point
│       └── desktopMain/           # Desktop app entry point
│
├── gradle/
│   └── libs.versions.toml         # Version catalog
├── build.gradle.kts               # Root build file
└── settings.gradle.kts            # Module includes
```

## Module Details

### PlatformSpellChecker (KMP Library)

A Kotlin Multiplatform library providing spell checking functionality.

#### Common API (`commonMain`)
- **PlatformSpellChecker.kt** - Expect class declaration
  - `suspend fun performSpellCheck(text: String): List<String>` - Check sentences
  - `suspend fun checkWord(word: String, maxSuggestions: Int = 5): List<String>` - Get suggestions for a single word (
    empty list if correct)

#### Android Implementation (`androidMain`)
- **PlatformSpellChecker.android.kt** - Actual implementation using Android's TextServicesManager
  - Uses `SpellCheckerSession` for spell checking
  - Tracks concurrent operations with cookies and ConcurrentHashMap
  - Supports both word-level and sentence-level spell checking

#### Desktop Implementation (`desktopMain`)
- **PlatformSpellChecker.desktop.kt** - Stubbed implementation
  - Returns placeholder messages
  - Ready for future implementation (Hunspell, LanguageTool, etc.)

### exampleApp (Compose Multiplatform App)

A sample application demonstrating the PlatformSpellChecker library.

#### Shared Code (`commonMain`)
- **App.kt** - Main composable entry point with Koin integration
- **SpellCheckViewModel.kt** - ViewModel managing UI state
  - `TabUiState` - Data class with text, suggestions, and loading state
  - `wordTabState` / `sentenceTabState` - StateFlows for each tab
- **ui/SpellCheckContent.kt** - Main container with TabRow navigation
- **ui/WordCheckTab.kt** - Single word checking tab with adaptive layout
- **ui/SentenceCheckTab.kt** - Sentence checking tab with adaptive layout
- **ui/theme/** - Material 3 theming (Color.kt, Theme.kt, Type.kt)

#### Android (`androidMain`)
- **MainActivity.kt** - Android Activity hosting the Compose UI
- **SpellCheckApplication.kt** - Application class initializing Koin
- **AndroidManifest.xml** - App manifest
- **res/** - Android resources (icons, themes, strings)

#### Desktop (`desktopMain`)
- **Main.kt** - Desktop application entry point
  - Creates window with Compose UI
  - Initializes Koin with desktop module

## Building and Running

### Android
```bash
./gradlew :exampleApp:assembleDebug
# Or run from Android Studio
```

### Desktop
```bash
./gradlew :exampleApp:run
# Or create distributable:
./gradlew :exampleApp:packageDistributionForCurrentOS
```

## How It Works

### Word Check Flow
1. User enters a word in the "Word" tab
2. ViewModel calls `spellChecker.checkWord()`
3. Platform implementation performs spell check
4. Results displayed: suggestions if misspelled, or an empty list if the word is correct

### Sentence Check Flow
1. User enters text in the "Sentence" tab
2. ViewModel calls `spellChecker.performSpellCheck()`
3. Platform implementation checks each word
4. Results show misspelled words with suggestions: "'word' → 'suggestion'"

## Key Features
- **Kotlin Multiplatform**: Share business logic across platforms
- **Compose Multiplatform**: Single UI codebase for Android and Desktop
- **Expect/Actual Pattern**: Platform-specific implementations with common API
- **Adaptive Layouts**: UI adjusts for landscape/portrait orientation
- **Reactive UI**: StateFlow ensures automatic UI updates
- **Dependency Injection**: Koin for clean architecture

## Dependencies (libs.versions.toml)
- Kotlin 2.1.0
- Compose Multiplatform 1.7.1
- Koin 4.0.0 (Multiplatform)
- Lifecycle ViewModel 2.8.4 (Multiplatform)
- Kotlinx Coroutines 1.9.0
