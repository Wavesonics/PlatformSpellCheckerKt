package com.darkrockstudios.libs.platformspellchecker.windows

import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.PointerByReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-level wrapper for the Windows Spell Checking API.
 *
 * This class provides a Kotlin-friendly interface to the Windows native spell checker,
 * which is available on Windows 8 and later.
 *
 * Usage:
 * ```
 * val spellChecker = WindowsSpellChecker.create("en-US")
 * if (spellChecker != null) {
 *     val errors = spellChecker.checkText("Helllo world")
 *     spellChecker.close()
 * }
 * ```
 */
class WindowsSpellChecker private constructor(
    private val spellChecker: ISpellChecker,
    val languageTag: String
) : AutoCloseable {

    private val closed = AtomicBoolean(false)

    /**
     * Checks the spelling of the provided text.
     *
     * @param text The text to check
     * @return List of spelling errors found
     */
    fun checkText(text: String): List<SpellingErrorInfo> {
        check(!closed.get()) { "WindowsSpellChecker has been closed" }

        if (text.isBlank()) {
            return emptyList()
        }

        val enumErrors = spellChecker.check(text)
        return try {
            enumErrors.toList()
        } finally {
            enumErrors.Release()
        }
    }

    /**
     * Gets spelling suggestions for a misspelled word.
     *
     * @param word The misspelled word
     * @return List of suggestions
     */
    fun getSuggestions(word: String): List<String> {
        check(!closed.get()) { "WindowsSpellChecker has been closed" }

        if (word.isBlank()) {
            return emptyList()
        }

        val enumSuggestions = spellChecker.suggest(word) ?: return emptyList()
        return try {
            enumSuggestions.toList()
        } finally {
            enumSuggestions.Release()
        }
    }

    /**
     * Checks if a single word is spelled correctly.
     *
     * @param word The word to check
     * @return true if spelled correctly, false otherwise
     */
    fun isWordCorrect(word: String): Boolean {
        check(!closed.get()) { "WindowsSpellChecker has been closed" }

        if (word.isBlank()) {
            return true
        }

        val errors = checkText(word)
        return errors.isEmpty()
    }

    /**
     * Adds a word to the user dictionary.
     *
     * @param word The word to add
     */
    fun addToDictionary(word: String) {
        check(!closed.get()) { "WindowsSpellChecker has been closed" }
        spellChecker.add(word)
    }

    /**
     * Ignores a word for this session.
     *
     * @param word The word to ignore
     */
    fun ignoreWord(word: String) {
        check(!closed.get()) { "WindowsSpellChecker has been closed" }
        spellChecker.ignore(word)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            spellChecker.Release()
        }
    }

    companion object {
        private val comInitialized = AtomicBoolean(false)
        private var factory: ISpellCheckerFactory? = null

        /**
         * Initializes COM and creates the spell checker factory.
         * This is called automatically when needed.
         */
        @Synchronized
        private fun ensureInitialized(): ISpellCheckerFactory? {
            if (factory != null) {
                return factory
            }

            try {
                // Initialize COM if not already done
                if (!comInitialized.get()) {
                    val hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED)
                    // S_OK or S_FALSE (already initialized) are both acceptable
                    val hrValue = hr.toLong().toInt()
                    if (hrValue != 0 && hrValue != 1) {
                        return null
                    }
                    comInitialized.set(true)
                }

                // Create the spell checker factory
                val ppFactory = PointerByReference()
                val hr = Ole32.INSTANCE.CoCreateInstance(
                    SpellCheckGUIDs.CLSID_SpellCheckerFactory,
                    null,
                    WTypes.CLSCTX_INPROC_SERVER,
                    SpellCheckGUIDs.IID_ISpellCheckerFactory,
                    ppFactory
                )

                if (COMUtils.FAILED(hr)) {
                    return null
                }

                factory = ISpellCheckerFactory(ppFactory.value)
                return factory
            } catch (e: Exception) {
                return null
            }
        }

        /**
         * Checks if a language is supported by the Windows spell checker.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return true if supported
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            val factory = ensureInitialized() ?: return false
            return try {
                factory.isSupported(languageTag)
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Creates a spell checker for the specified language.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return WindowsSpellChecker instance, or null if creation failed
         */
        fun create(languageTag: String): WindowsSpellChecker? {
            val factory = ensureInitialized() ?: return null

            return try {
                if (!factory.isSupported(languageTag)) {
                    return null
                }
                val spellChecker = factory.createSpellChecker(languageTag)
                WindowsSpellChecker(spellChecker, languageTag)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Creates a spell checker using the system's default language.
         *
         * @return WindowsSpellChecker instance, or null if creation failed
         */
        fun createDefault(): WindowsSpellChecker? {
            val defaultLocale = java.util.Locale.getDefault()
            val languageTag = defaultLocale.toLanguageTag()
            return create(languageTag) ?: create("en-US")
        }

        /**
         * Gets a list of common language tags to try.
         */
        fun getCommonLanguages(): List<String> = listOf(
            "en-US", "en-GB", "en-AU", "en-CA",
            "es-ES", "es-MX",
            "fr-FR", "fr-CA",
            "de-DE", "de-AT", "de-CH",
            "it-IT",
            "pt-BR", "pt-PT",
            "nl-NL",
            "pl-PL",
            "ru-RU",
            "ja-JP",
            "zh-CN", "zh-TW",
            "ko-KR"
        )

        /**
         * Finds the first supported language from a list of preferred languages.
         */
        fun findSupportedLanguage(preferred: List<String>): String? {
            return preferred.firstOrNull { isLanguageSupported(it) }
        }
    }
}
