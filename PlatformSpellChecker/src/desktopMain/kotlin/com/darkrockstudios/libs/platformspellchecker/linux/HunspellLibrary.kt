package com.darkrockstudios.libs.platformspellchecker.linux

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference

/**
 * JNA bindings for the Hunspell spell checking library.
 *
 * Hunspell is the most widely used spell checker on Linux systems.
 * It's used by LibreOffice, Firefox, Chrome, and many other applications.
 *
 * Reference: https://github.com/hunspell/hunspell/blob/master/src/hunspell/hunspell.h
 */
interface HunspellLibrary : Library {

    /**
     * Creates a new Hunspell instance from dictionary files.
     *
     * @param affpath Path to the .aff (affix) file
     * @param dpath Path to the .dic (dictionary) file
     * @return Handle to the Hunspell instance, or null on failure
     */
    fun Hunspell_create(affpath: String, dpath: String): Pointer?

    /**
     * Creates a new Hunspell instance from dictionary files with a custom key.
     *
     * @param affpath Path to the .aff (affix) file
     * @param dpath Path to the .dic (dictionary) file
     * @param key Encryption key (usually null for standard dictionaries)
     * @return Handle to the Hunspell instance, or null on failure
     */
    fun Hunspell_create_key(affpath: String, dpath: String, key: String?): Pointer?

    /**
     * Destroys a Hunspell instance and frees all resources.
     *
     * @param pHunspell Handle to the Hunspell instance
     */
    fun Hunspell_destroy(pHunspell: Pointer)

    /**
     * Checks if a word is spelled correctly.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param word The word to check
     * @return Non-zero if the word is correct, 0 if misspelled
     */
    fun Hunspell_spell(pHunspell: Pointer, word: String): Int

    /**
     * Gets spelling suggestions for a word.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param slst Pointer to receive the array of suggestions
     * @param word The word to get suggestions for
     * @return Number of suggestions returned
     */
    fun Hunspell_suggest(pHunspell: Pointer, slst: PointerByReference, word: String): Int

    /**
     * Frees a list of suggestions returned by Hunspell_suggest.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param slst Pointer to the suggestion array
     * @param n Number of suggestions in the array
     */
    fun Hunspell_free_list(pHunspell: Pointer, slst: PointerByReference, n: Int)

    /**
     * Adds a word to the runtime dictionary.
     * The word is only remembered for this session and not persisted.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param word The word to add
     * @return 0 on success
     */
    fun Hunspell_add(pHunspell: Pointer, word: String): Int

    /**
     * Adds a word with affix flags to the runtime dictionary.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param word The word to add
     * @param flags Affix flags to apply
     * @return 0 on success
     */
    fun Hunspell_add_with_affix(pHunspell: Pointer, word: String, flags: String): Int

    /**
     * Removes a word from the runtime dictionary.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @param word The word to remove
     * @return 0 on success
     */
    fun Hunspell_remove(pHunspell: Pointer, word: String): Int

    /**
     * Gets the dictionary encoding.
     *
     * @param pHunspell Handle to the Hunspell instance
     * @return The encoding string (e.g., "UTF-8")
     */
    fun Hunspell_get_dic_encoding(pHunspell: Pointer): String?

    companion object {
        /**
         * Library names to try loading, in order of preference.
         * Different distros may have different library names/versions.
         */
        private val LIBRARY_NAMES = listOf(
            "hunspell-1.7",      // Debian/Ubuntu recent
            "hunspell-1.6",      // Older systems
            "hunspell-1.5",
            "hunspell-1.4",
            "hunspell-1.3",
            "hunspell"           // Generic fallback
        )

        /**
         * Attempts to load the Hunspell library.
         *
         * @return The loaded library instance, or null if not available
         */
        fun load(): HunspellLibrary? {
            for (name in LIBRARY_NAMES) {
                try {
                    return Native.load(name, HunspellLibrary::class.java)
                } catch (e: UnsatisfiedLinkError) {
                    // Try next name
                }
            }
            return null
        }

        /**
         * Checks if the Hunspell library is available on this system.
         */
        fun isAvailable(): Boolean {
            return load() != null
        }
    }
}
