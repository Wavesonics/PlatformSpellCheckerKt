# Platform Spell Checker Kotlin


[![Maven Central](https://img.shields.io/maven-central/v/com.darkrockstudios/platform-spellcheckerkt.svg)](https://search.maven.org/artifact/com.darkrockstudios/platform-spellcheckerkt)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

[![KMP](https://img.shields.io/badge/platforms:-blue.svg?logo=kotlin)](http://kotlinlang.org)
![badge-jvm] ![badge-android] ![badge-ios]


This Kotlin Multiplatform library wraps the OS's native spell checker
on each supported platform. It then provides a Kotlin friendly API.

The big advantage here is that nothing needs to be bundled with your program.
Especially dictionaries. Most likely, what ever the user's language is, they will
already have the proper dictionary on their device for the system level Spell Checker.

### Dependency

```kotlin
implementation("com.darkrockstudios:platform-spellcheckerkt:0.9.0")
```

## Usage

The simplest way to get a spell checker instance is through the PlatformSpellCheckerFactory.
This can be used in common code, but if you support Android, you will need to construct it in Platform specific code,
and then pass it down into common code, because the Android varriant requires a `context`.

```kotlin
val factory = PlatformSpellCheckerFactory()

// Use the user's current/system language
val checkerDefault = factory.createSpellChecker()

// Or request a specific locale (validated). Country is optional.
val checkerUkUa = factory.createSpellChecker(SpLocale.UK_UA)

// Check before you create! Creating a Spell Checker for a locale that isn't supported will throw an exception
if (factory.hasLanguage(SpLocale.EN_GB)) {
    val gbChecker = factory.createSpellChecker(SpLocale.EN_GB)
}

// Use the checker
val result = checkerDefault.checkWord("mispelledWord")
if(isMisspelled(result)) {
    result.suggestions.forEach { println(it) }
}


```

You can also query some helpful utilities via the factory:
```kotlin
val factory = PlatformSpellCheckerFactory()

// Is a spell checker available on this platform at runtime?
val available = factory.isAvailable()

// What locale does the system currently use?
val systemLocale: SpLocale = factory.currentSystemLocale()

// A best-effort list of available Spell Checker locales on this device
val locales: List<SpLocale> = factory.availableLocales()
```

## Supported Platforms

| Platform | Supported | Implementation Details                                                                                                                                                                                                                                                                                                                                                    |
|----------|-----------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Android | ✅ | Wraps `TextServicesManager`'s `SpellCheckerSession` with full featured support.                                                                                                                                                                                                                                                                                           |
| iOS | ✅ | Wraps `UITextChecker` for full featured support.                                                                                                                                                                                                                                                                                                                          |
| JVM - Windows | ✅ | Wraps the Win32 API COM Object for `ISpellCheckerFactory` for full featured support.                                                                                                                                                                                                                                                                                      |
| JVM - Linux | ✅ | Linux has no native spell checker API, so this library looks to see if `hunspell` is installed, and uses it, otherwise it reports Spell Checking is not supported. Single word spell checking is a first class citizen, but sentence level correct is really just decomposing the sentence into individual single word checks, so is not as robust as other platforms.    |
| JVM - macOS | ⚠️ | Wraps `NSSpellChecker` for partial support.<br/>**Known issues:**<br/>• There is an issue with JNA calling into certain platform APIs, so sentence level correct is hacked together right now.<br>• MacOS has a bit of a weird spell checker, the words it considers "correct" are not always what I would expect, but who am I to question the wisdom of Tim Apple. |
| K/N - Winx86 | ❓ | Support is certainly possible, but there aren't any examples I've found for binding COM objects in K/N, so would take some exploration.                                                                                                                                                                                                                                   |
| WASM-JS | ❌ | Can't work I don't think because there is no standard browser spell checker API as far as I have found.                                                                                                                                                                                                                                                                   |


[badge-android]: http://img.shields.io/badge/-android-6EDB8D.svg?style=flat

[badge-jvm]: http://img.shields.io/badge/-jvm-DB413D.svg?style=flat

[badge-linux]: http://img.shields.io/badge/-linux-2D3F6C.svg?style=flat

[badge-windows]: http://img.shields.io/badge/-windows-4D76CD.svg?style=flat

[badge-wasm]: https://img.shields.io/badge/-wasm-624FE8.svg?style=flat

[badge-wasmi]: https://img.shields.io/badge/-wasi-626FFF.svg?style=flat

[badge-apple-silicon]: http://img.shields.io/badge/support-[AppleSilicon]-43BBFF.svg?style=flat

[badge-ios]: http://img.shields.io/badge/-ios-CDCDCD.svg?style=flat

[badge-ios-sim]: http://img.shields.io/badge/-iosSim-AFAFAF.svg?style=flat

[badge-macos]: http://img.shields.io/badge/-macos-444444.svg?style=flat

[badge-watchos]: http://img.shields.io/badge/-watchos-C0C0C0.svg?style=flat

[badge-tvos]: http://img.shields.io/badge/-tvos-808080.svg?style=flat
