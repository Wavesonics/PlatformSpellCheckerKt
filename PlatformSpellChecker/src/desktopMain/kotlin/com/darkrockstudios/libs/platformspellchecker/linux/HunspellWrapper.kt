package com.darkrockstudios.libs.platformspellchecker.linux

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import io.github.aakira.napier.Napier
import java.io.File
import java.nio.charset.Charset
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Wrapper around the Hunspell library providing a cleaner Kotlin API.
 *
 * Handles:
 * - Dictionary file discovery from standard system paths
 * - User dictionary support (~/.local/share/hunspell/)
 * - Proper encoding handling
 * - Resource cleanup
 */
class HunspellWrapper private constructor(
    private val library: HunspellLibrary,
    private val handle: Pointer,
    val languageTag: String,
    private val encoding: Charset
) : AutoCloseable {

    private val closed = AtomicBoolean(false)
    private val ignoredWords = mutableSetOf<String>()
    private val userDictionaryPath: File? = getUserDictionaryPath(languageTag)

    /**
     * Checks if a word is spelled correctly.
     */
    fun isWordCorrect(word: String): Boolean {
        check(!closed.get()) { "HunspellWrapper has been closed" }

        if (word.isBlank()) return true
        if (ignoredWords.contains(word.lowercase())) return true

        return library.Hunspell_spell(handle, word) != 0
    }

    /**
     * Gets spelling suggestions for a misspelled word.
     *
     * @param word The word to get suggestions for
     * @param maxSuggestions Maximum number of suggestions to return (default 10)
     * @return List of spelling suggestions
     */
    fun getSuggestions(word: String, maxSuggestions: Int = 10): List<String> {
        check(!closed.get()) { "HunspellWrapper has been closed" }

        if (word.isBlank()) return emptyList()

        val slstRef = PointerByReference()
        val count = library.Hunspell_suggest(handle, slstRef, word)

        if (count <= 0) return emptyList()

        return try {
            val slst = slstRef.value
            if (slst == Pointer.NULL) return emptyList()

            val suggestions = mutableListOf<String>()
            for (i in 0 until minOf(count, maxSuggestions)) {
                val strPtr = slst.getPointer((i * Native.POINTER_SIZE).toLong())
                if (strPtr != null && strPtr != Pointer.NULL) {
                    val suggestion = strPtr.getString(0, encoding.name())
                    suggestions.add(suggestion)
                }
            }
            suggestions
        } finally {
            library.Hunspell_free_list(handle, slstRef, count)
        }
    }

    /**
     * Adds a word to the runtime dictionary.
     * The word is remembered for this session only.
     */
    fun addWord(word: String) {
        check(!closed.get()) { "HunspellWrapper has been closed" }
        if (word.isBlank()) return

        library.Hunspell_add(handle, word)
    }

    /**
     * Adds a word to the user's personal dictionary.
     * The word is persisted to disk and will be available in future sessions.
     */
    fun addToUserDictionary(word: String) {
        check(!closed.get()) { "HunspellWrapper has been closed" }
        if (word.isBlank()) return

        // Add to runtime dictionary
        addWord(word)

        // Persist to user dictionary file
        userDictionaryPath?.let { dictFile ->
            try {
                dictFile.parentFile?.mkdirs()
                dictFile.appendText("$word\n")
                Napier.d("Added '$word' to user dictionary: $dictFile")
            } catch (e: Exception) {
                Napier.e("Failed to write to user dictionary: ${e.message}", e)
            }
        }
    }

    /**
     * Ignores a word for this session.
     */
    fun ignoreWord(word: String) {
        if (word.isBlank()) return
        ignoredWords.add(word.lowercase())
    }

    /**
     * Removes a word from the runtime dictionary.
     */
    fun removeWord(word: String) {
        check(!closed.get()) { "HunspellWrapper has been closed" }
        if (word.isBlank()) return

        library.Hunspell_remove(handle, word)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            library.Hunspell_destroy(handle)
        }
    }

    companion object {
        /**
         * Standard paths to search for dictionary files on Linux.
         */
        val DICTIONARY_PATHS = listOf(
            "/usr/share/hunspell",
            "/usr/share/myspell",
            "/usr/share/myspell/dicts",
            "/usr/local/share/hunspell",
            "/usr/local/share/myspell"
        )

        /**
         * Gets the user dictionary directory.
         */
        private fun getUserDictionaryDir(): File {
            val home = System.getProperty("user.home")
            return File(home, ".local/share/hunspell")
        }

        /**
         * Gets the path for a user's personal dictionary file.
         */
        private fun getUserDictionaryPath(languageTag: String): File {
            return File(getUserDictionaryDir(), "${languageTag.replace("-", "_")}_user.dic")
        }

        /**
         * Finds dictionary files (.aff and .dic) for a language tag.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return Pair of (affPath, dicPath) or null if not found
         */
        fun findDictionaryFiles(languageTag: String): Pair<String, String>? {
            // Convert BCP47 to Hunspell format (e.g., en-US -> en_US)
            val hunspellName = languageTag.replace("-", "_")

            // Also try just the language part (e.g., en)
            val languagePart = languageTag.split("-", "_").firstOrNull() ?: languageTag

            // Search in all standard paths
            val searchPaths = DICTIONARY_PATHS + getUserDictionaryDir().absolutePath

            for (basePath in searchPaths) {
                val dir = File(basePath)
                if (!dir.exists() || !dir.isDirectory) continue

                // Try exact match first (e.g., en_US)
                val exactAff = File(dir, "$hunspellName.aff")
                val exactDic = File(dir, "$hunspellName.dic")
                if (exactAff.exists() && exactDic.exists()) {
                    return Pair(exactAff.absolutePath, exactDic.absolutePath)
                }

                // Try language-only match (e.g., en)
                val langAff = File(dir, "$languagePart.aff")
                val langDic = File(dir, "$languagePart.dic")
                if (langAff.exists() && langDic.exists()) {
                    return Pair(langAff.absolutePath, langDic.absolutePath)
                }

                // Try to find any dictionary starting with the language part
                val affFiles = dir.listFiles { _, name ->
                    name.startsWith(languagePart) && name.endsWith(".aff")
                }
                if (affFiles != null && affFiles.isNotEmpty()) {
                    val affFile = affFiles.first()
                    val dicFile = File(dir, affFile.name.replace(".aff", ".dic"))
                    if (dicFile.exists()) {
                        return Pair(affFile.absolutePath, dicFile.absolutePath)
                    }
                }
            }

            return null
        }

        /**
         * Gets a list of all available dictionary language tags.
         */
        fun getAvailableLanguages(): List<String> {
            val languages = mutableSetOf<String>()

            for (basePath in DICTIONARY_PATHS) {
                val dir = File(basePath)
                if (!dir.exists() || !dir.isDirectory) continue

                dir.listFiles { _, name -> name.endsWith(".dic") }?.forEach { file ->
                    val lang = file.name.removeSuffix(".dic")
                    // Convert back to BCP47 format
                    languages.add(lang.replace("_", "-"))
                }
            }

            return languages.toList().sorted()
        }

        /**
         * Checks if a language is supported (dictionary files exist).
         */
        fun isLanguageSupported(languageTag: String): Boolean {
            return findDictionaryFiles(languageTag) != null
        }

        /**
         * Creates a HunspellWrapper for the specified language.
         *
         * @param languageTag BCP47 language tag (e.g., "en-US")
         * @return HunspellWrapper instance, or null if creation failed
         */
        fun create(languageTag: String): HunspellWrapper? {
            val library = HunspellLibrary.load()
            if (library == null) {
                Napier.e("Failed to load Hunspell library")
                return null
            }

            val dictFiles = findDictionaryFiles(languageTag)
            if (dictFiles == null) {
                Napier.e("No dictionary files found for language: $languageTag")
                return null
            }

            val (affPath, dicPath) = dictFiles
            Napier.d("Creating Hunspell with aff=$affPath, dic=$dicPath")

            val handle = library.Hunspell_create(affPath, dicPath)
            if (handle == null || handle == Pointer.NULL) {
                Napier.e("Hunspell_create failed for $languageTag")
                return null
            }

            // Get the dictionary encoding
            val encodingStr = library.Hunspell_get_dic_encoding(handle)
            val encoding = try {
                if (encodingStr != null) Charset.forName(encodingStr) else Charsets.UTF_8
            } catch (e: Exception) {
                Napier.w("Unknown encoding $encodingStr, using UTF-8")
                Charsets.UTF_8
            }

            val wrapper = HunspellWrapper(library, handle, languageTag, encoding)

            // Load user dictionary words
            wrapper.loadUserDictionary()

            return wrapper
        }

        /**
         * Creates a HunspellWrapper using the system's default language.
         */
        fun createDefault(): HunspellWrapper? {
            // Try system locale first
            val defaultLocale = Locale.getDefault()
            val languageTag = defaultLocale.toLanguageTag()

            // Try full tag (e.g., en-US)
            create(languageTag)?.let { return it }

            // Try just language (e.g., en)
            val language = defaultLocale.language
            create(language)?.let { return it }

            // Fall back to en-US
            create("en-US")?.let { return it }

            // Try any available language
            val available = getAvailableLanguages()
            if (available.isNotEmpty()) {
                return create(available.first())
            }

            return null
        }
    }

    /**
     * Loads words from the user's personal dictionary file.
     */
    private fun loadUserDictionary() {
        userDictionaryPath?.let { dictFile ->
            if (dictFile.exists()) {
                try {
                    dictFile.readLines()
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .forEach { word ->
                            library.Hunspell_add(handle, word)
                        }
                    Napier.d("Loaded user dictionary from: $dictFile")
                } catch (e: Exception) {
                    Napier.e("Failed to load user dictionary: ${e.message}", e)
                }
            }
        }
    }
}
