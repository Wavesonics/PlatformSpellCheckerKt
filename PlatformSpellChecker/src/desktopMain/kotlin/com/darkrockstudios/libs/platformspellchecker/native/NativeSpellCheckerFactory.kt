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
        OperatingSystem.MACOS -> true
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
        OperatingSystem.WINDOWS -> WindowsSpellChecker.create(languageTag)
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
        OperatingSystem.WINDOWS -> WindowsSpellChecker.createDefault()
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