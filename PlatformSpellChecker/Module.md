# Module PlatformSpellChecker

A Kotlin Multiplatform spell checker that wraps each operating system's **native** spell-checking
engine behind one common Kotlin API — Android's `TextServicesManager`, iOS/macOS's `NSSpellChecker` /
`UITextChecker`, Windows' `ISpellChecker` COM API, and Hunspell on Linux. Nothing is bundled: the
library uses whatever dictionaries the user's device already has for their language.

> Results differ by platform — each OS decides what counts as "correct" and what to suggest. The
> library surfaces the native answer without trying to reconcile them.

## Create a checker

Go through `PlatformSpellCheckerFactory`. On Android the factory needs a `Context`, so build it in
platform code and pass it into common code; on Desktop and iOS the no-arg constructor works.

```kotlin
val factory = PlatformSpellCheckerFactory()             // Desktop / iOS
// val factory = PlatformSpellCheckerFactory(context)   // Android

if (!factory.isAvailable()) return                      // no native checker on this device

val checker = factory.createSpellChecker()              // the system's current language

// Validate a locale before requesting it — creating a checker for an unsupported locale throws.
val gb = if (factory.hasLanguage(SpLocale.EN_GB)) factory.createSpellChecker(SpLocale.EN_GB) else null
```

## Check a single word

```kotlin
when (val result = checker.checkWord("mispelled")) {
    is CorrectWord    -> { /* spelled correctly */ }
    is MisspelledWord -> result.suggestions.forEach(::println)   // closest first; may be empty
}
```

## Check a sentence

```kotlin
val corrections = checker.checkMultiword("This is a mispelled sentance.")
corrections.forEach { c ->
    println("${c.misspelledWord} @ ${c.startIndex} (len ${c.length}) -> ${c.suggestions}")
}
```

## Manage the user dictionary

```kotlin
checker.addToDictionary("Kotlin")                            // AppLocal (in-memory) by default
checker.addToDictionary("Darkrock", DictionaryScope.System)  // OS-level learn, where supported
checker.ignoreWord("typooo")                                 // ignored for this session only

val custom: Set<String> = checker.userDictionary()           // persist this yourself if you need to
checker.setUserDictionary(custom)                            // restore it into a fresh checker

checker.close()                                              // release native resources when done
```

`DictionaryScope.AppLocal` lives only for the lifetime of this checker; `DictionaryScope.System` uses
the platform's native learn API (OS-wide on iOS/macOS, per-user on Windows/Linux). Android has no
native learn API and falls back to `AppLocal`.

## Locales

`SpLocale` is a small `language` + optional `country` value with constants for the common cases
(`SpLocale.EN_US`, `SpLocale.EN_GB`, `SpLocale.DE_DE`, …). Use `factory.currentSystemLocale()` and
`factory.availableLocales()` to discover what the device offers.

## Platform support

| Platform | Backend |
|---|---|
| Android | `TextServicesManager` / `SpellCheckerSession` |
| iOS | `UITextChecker` |
| JVM — Windows | `ISpellChecker` (Win32 COM) |
| JVM — macOS | `NSSpellChecker` (partial) |
| JVM — Linux | Hunspell, if installed |

# Package com.darkrockstudios.libs.platformspellchecker

The public API: `PlatformSpellCheckerFactory`, `PlatformSpellChecker`, the `WordCheckResult` hierarchy
(`CorrectWord` / `MisspelledWord`), `SpellingCorrection`, `SpLocale`, and `DictionaryScope`.

# Package com.darkrockstudios.libs.platformspellchecker.native

Desktop (JVM) backend abstraction: `NativeSpellChecker` and `NativeSpellCheckerFactory`, which pick a
Windows, macOS, or Linux implementation at runtime. Most consumers use the common API above instead.

# Package com.darkrockstudios.libs.platformspellchecker.windows

Windows desktop backend — `ISpellChecker` COM interop behind `NativeSpellChecker`.

# Package com.darkrockstudios.libs.platformspellchecker.macos

macOS desktop backend — `NSSpellChecker` via the Objective-C runtime (JNA).

# Package com.darkrockstudios.libs.platformspellchecker.linux

Linux desktop backend — Hunspell bindings (JNA).
