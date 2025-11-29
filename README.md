# Platform Spell Checker Kotlin


[![Maven Central](https://img.shields.io/maven-central/v/com.darkrockstudios/platform-spellcheckerkt.svg)](https://search.maven.org/artifact/com.darkrockstudios/platform-spellcheckerkt)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

[![KMP](https://img.shields.io/badge/platforms:-blue.svg?logo=kotlin)](http://kotlinlang.org)
![badge-jvm] ![badge-android] ![badge-ios]


This Kotlin Multiplatform library wraps the OS's native spell checker
on each supported platform. It then provides a Kotlin friendly API.

## Usage

```kotlin
class PlatformSpellChecker {
    /**
     * Performs spell check on a sentence or multi-word text.
     * Returns a list of [SpellingCorrection] objects containing the misspelled words,
     * their positions in the original text, and suggested corrections.
     * Returns an empty list if no spelling errors are found.
     */
    suspend fun performSpellCheck(text: String): List<SpellingCorrection>

    /**
     * Checks a single word for spelling errors.
     * Returns spelling suggestions if the word is misspelled,
     * or "'word' is correctly spelled" if the word is correct.
     */
    suspend fun checkWord(word: String): List<String>
}
```

```kotlin
val spellChecker = PlatformSpellChecker()

spellChecker.checkWord("mispelledWord").forEach { suggestion ->
    println(suggestion)
}
```

## Supported Platforms

* Android
* iOS
* JVM
  * Linux
  * macOS
    * **Known issues:**
      * _There an issue with JNA calling into certain platform APIs, so sentence level correct is sorta hacked together right now_
      * _MacOS has a bit of a weird spell checker, the words it considers "correct" are not always what I would expect, but who am I to question the wisdom of Tim Apple._
  * Windows

## Possible Future Platforms
* **Kotlin/Native** support is certainly possible for the desktop platforms, but I haven't had need for it yet.

* **WASM/JS** can't work I don't think because there is no standard browser spell checker API as far as I have found.

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