package com.darkrockstudios.libs.platformspellchecker.native

import com.darkrockstudios.libs.platformspellchecker.windows.WindowsSpellChecker

/**
 * Factory for creating platform-specific spell checkers.
 *
 * Automatically detects the operating system and creates the appropriate
 * NativeSpellChecker implementation.
 */
object NativeSpellCheckerFactory {

    /**
     * The detected operating system.
     */
    val currentOS: OperatingSystem by lazy {
        val osName = System.getProperty("os.name").lowercase()
        when {
            osName.contains("windows") -> OperatingSystem.WINDOWS
            osName.contains("mac") || osName.contains("darwin") -> OperatingSystem.MACOS
            osName.contains("linux") -> OperatingSystem.LINUX
            osName.contains("freebsd") || osName.contains("openbsd") -> OperatingSystem.LINUX // Treat BSD as Linux-like
            else -> OperatingSystem.UNKNOWN
        }
    }

    /**
     * Checks if spell checking is available on this platform.
     *
     * @return true if a spell checker implementation exists for this OS
     */
    fun isAvailable(): Boolean = when (currentOS) {
        OperatingSystem.WINDOWS -> true
        OperatingSystem.MACOS -> false // Not yet implemented
        OperatingSystem.LINUX -> false // Not yet implemented
        OperatingSystem.UNKNOWN -> false
    }

    /**
     * Checks if a language is supported on the current platform.
     *
     * @param languageTag BCP47 language tag (e.g., "en-US")
     * @return true if the language is supported
     */
    fun isLanguageSupported(languageTag: String): Boolean = when (currentOS) {
        OperatingSystem.WINDOWS -> WindowsSpellChecker.isLanguageSupported(languageTag)
        OperatingSystem.MACOS -> MacOSSpellChecker.isLanguageSupported(languageTag)
        OperatingSystem.LINUX -> LinuxSpellChecker.isLanguageSupported(languageTag)
        OperatingSystem.UNKNOWN -> false
    }

    /**
     * Creates a spell checker for the specified language.
     *
     * @param languageTag BCP47 language tag (e.g., "en-US")
     * @return NativeSpellChecker instance, or null if creation failed
     */
    fun create(languageTag: String): NativeSpellChecker? = when (currentOS) {
        OperatingSystem.WINDOWS -> WindowsSpellChecker.create(languageTag)?.asNativeSpellChecker()
        OperatingSystem.MACOS -> MacOSSpellChecker.create(languageTag)
        OperatingSystem.LINUX -> LinuxSpellChecker.create(languageTag)
        OperatingSystem.UNKNOWN -> null
    }

    /**
     * Creates a spell checker using the system's default language.
     *
     * @return NativeSpellChecker instance, or null if creation failed
     */
    fun createDefault(): NativeSpellChecker? = when (currentOS) {
        OperatingSystem.WINDOWS -> WindowsSpellChecker.createDefault()?.asNativeSpellChecker()
        OperatingSystem.MACOS -> MacOSSpellChecker.createDefault()
        OperatingSystem.LINUX -> LinuxSpellChecker.createDefault()
        OperatingSystem.UNKNOWN -> null
    }

    /**
     * Gets the name of the current operating system.
     */
    fun getOSName(): String = when (currentOS) {
        OperatingSystem.WINDOWS -> "Windows"
        OperatingSystem.MACOS -> "macOS"
        OperatingSystem.LINUX -> "Linux"
        OperatingSystem.UNKNOWN -> "Unknown"
    }
}

/**
 * Supported operating systems.
 */
enum class OperatingSystem {
    WINDOWS,
    MACOS,
    LINUX,
    UNKNOWN
}

/**
 * Extension function to adapt WindowsSpellChecker to NativeSpellChecker interface.
 */
private fun WindowsSpellChecker.asNativeSpellChecker(): NativeSpellChecker {
    return WindowsSpellCheckerAdapter(this)
}

/**
 * Adapter to make WindowsSpellChecker compatible with NativeSpellChecker interface.
 */
private class WindowsSpellCheckerAdapter(
    private val delegate: WindowsSpellChecker
) : NativeSpellChecker {

    override val languageTag: String
        get() = delegate.languageTag

    override fun checkText(text: String): List<SpellingError> {
        return delegate.checkText(text).map { error ->
            SpellingError(
                startIndex = error.startIndex,
                length = error.length,
                correctiveAction = when (error.correctiveAction) {
                    com.darkrockstudios.libs.platformspellchecker.windows.CorrectiveAction.NONE -> CorrectiveAction.NONE
                    com.darkrockstudios.libs.platformspellchecker.windows.CorrectiveAction.GET_SUGGESTIONS -> CorrectiveAction.GET_SUGGESTIONS
                    com.darkrockstudios.libs.platformspellchecker.windows.CorrectiveAction.REPLACE -> CorrectiveAction.REPLACE
                    com.darkrockstudios.libs.platformspellchecker.windows.CorrectiveAction.DELETE -> CorrectiveAction.DELETE
                    else -> CorrectiveAction.GET_SUGGESTIONS
                },
                replacement = error.replacement
            )
        }
    }

    override fun getSuggestions(word: String): List<String> {
        return delegate.getSuggestions(word)
    }

    override fun isWordCorrect(word: String): Boolean {
        return delegate.isWordCorrect(word)
    }

    override fun addToDictionary(word: String) {
        delegate.addToDictionary(word)
    }

    override fun ignoreWord(word: String) {
        delegate.ignoreWord(word)
    }

    override fun close() {
        delegate.close()
    }
}
