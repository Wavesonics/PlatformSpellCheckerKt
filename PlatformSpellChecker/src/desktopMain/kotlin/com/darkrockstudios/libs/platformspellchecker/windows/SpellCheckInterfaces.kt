package com.darkrockstudios.libs.platformspellchecker.windows

import com.sun.jna.Pointer
import com.sun.jna.WString
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.PointerByReference

/**
 * JNA wrapper for ISpellCheckerFactory COM interface.
 *
 * VTable layout (after IUnknown):
 * 3: get_SupportedLanguages
 * 4: IsSupported
 * 5: CreateSpellChecker
 */
class ISpellCheckerFactory(pvInstance: Pointer?) : Unknown(pvInstance) {

    /**
     * Determines if the specified language is supported by a registered spell checker.
     *
     * @param languageTag BCP47 language tag (e.g., "en-US")
     * @return true if supported, false otherwise
     */
    fun isSupported(languageTag: String): Boolean {
        val isSupported = IntByReference()
        val wstr = WString(languageTag)
        val hr = _invokeNativeInt(
            4, // VTable index for IsSupported
            arrayOf(pointer, wstr, isSupported)
        )
        COMUtils.checkRC(HRESULT(hr))
        return isSupported.value != 0
    }

    /**
     * Creates a spell checker for the specified language.
     *
     * @param languageTag BCP47 language tag (e.g., "en-US")
     * @return ISpellChecker instance
     */
    fun createSpellChecker(languageTag: String): ISpellChecker {
        val ppSpellChecker = PointerByReference()
        val wstr = WString(languageTag)
        val hr = _invokeNativeInt(
            5, // VTable index for CreateSpellChecker
            arrayOf(pointer, wstr, ppSpellChecker)
        )
        COMUtils.checkRC(HRESULT(hr))
        return ISpellChecker(ppSpellChecker.value)
    }
}

/**
 * JNA wrapper for ISpellChecker COM interface.
 *
 * VTable layout (from rust-winapi spellcheck.rs):
 * 0: QueryInterface (IUnknown)
 * 1: AddRef (IUnknown)
 * 2: Release (IUnknown)
 * 3: get_LanguageTag
 * 4: Check
 * 5: Suggest
 * 6: Add
 * 7: Ignore
 * 8: AutoCorrect
 * 9: GetOptionValue
 * 10: Get_OptionIds
 * 11: Get_Id
 * 12: Get_LocalizedName
 * 13: add_SpellCheckerChanged
 * 14: remove_SpellCheckerChanged
 * 15: GetOptionDescription
 * 16: ComprehensiveCheck
 */
class ISpellChecker(pvInstance: Pointer?) : Unknown(pvInstance) {

    /**
     * Checks the spelling of the supplied text.
     *
     * @param text The text to check
     * @return IEnumSpellingError containing spelling errors (empty if no errors)
     */
    fun check(text: String): IEnumSpellingError {
        val ppErrors = PointerByReference()
        val wstr = WString(text)
        val hr = _invokeNativeInt(
            4, // VTable index for Check
            arrayOf(pointer, wstr, ppErrors)
        )
        COMUtils.checkRC(HRESULT(hr))
        return IEnumSpellingError(ppErrors.value)
    }

    /**
     * Gets suggestions for the specified word.
     *
     * @param word The word to get suggestions for
     * @return IEnumString containing suggestions, or null if none
     */
    fun suggest(word: String): IEnumString? {
        val ppSuggestions = PointerByReference()
        val wstr = WString(word)
        val hr = _invokeNativeInt(
            5, // VTable index for Suggest
            arrayOf(pointer, wstr, ppSuggestions)
        )
        COMUtils.checkRC(HRESULT(hr))
        return if (ppSuggestions.value != null) IEnumString(ppSuggestions.value) else null
    }

    /**
     * Adds a word to the user dictionary.
     *
     * @param word The word to add
     */
    fun add(word: String) {
        val wstr = WString(word)
        val hr = _invokeNativeInt(
            6, // VTable index for Add (correct index)
            arrayOf(pointer, wstr)
        )
        COMUtils.checkRC(HRESULT(hr))
    }

    /**
     * Ignores a word for this session.
     *
     * @param word The word to ignore
     */
    fun ignore(word: String) {
        val wstr = WString(word)
        val hr = _invokeNativeInt(
            7, // VTable index for Ignore (correct index)
            arrayOf(pointer, wstr)
        )
        COMUtils.checkRC(HRESULT(hr))
    }
}

/**
 * JNA wrapper for IEnumSpellingError COM interface.
 *
 * VTable layout (after IUnknown):
 * 3: Next
 */
class IEnumSpellingError(pvInstance: Pointer?) : Unknown(pvInstance) {

    /**
     * Gets the next spelling error.
     *
     * IEnumSpellingError::Next signature: HRESULT Next([out] ISpellingError **value)
     *
     * @return ISpellingError or null if no more errors
     */
    fun next(): ISpellingError? {
        if (this.pointer == null) {
            return null
        }

        val ppError = PointerByReference()
        val hr = _invokeNativeInt(
            3, // VTable index for Next
            arrayOf(getPointer(), ppError)
        )

        // S_OK (0) means success, S_FALSE (1) means no more items
        return if (hr == 0 && ppError.value != null) {
            ISpellingError(ppError.value)
        } else {
            null
        }
    }

    /**
     * Iterates over all spelling errors.
     */
    fun forEach(action: (ISpellingError) -> Unit) {
        var error = next()
        while (error != null) {
            try {
                action(error)
            } finally {
                error.Release()
            }
            error = next()
        }
    }

    /**
     * Collects all spelling errors into a list.
     */
    fun toList(): List<SpellingErrorInfo> {
        val errors = mutableListOf<SpellingErrorInfo>()
        forEach { error ->
            errors.add(
                SpellingErrorInfo(
                    startIndex = error.getStartIndex(),
                    length = error.getLength(),
                    correctiveAction = error.getCorrectiveAction(),
                    replacement = error.getReplacement()
                )
            )
        }
        return errors
    }
}

/**
 * Data class to hold spelling error information.
 */
data class SpellingErrorInfo(
    val startIndex: Int,
    val length: Int,
    val correctiveAction: Int,
    val replacement: String?
)

/**
 * JNA wrapper for ISpellingError COM interface.
 *
 * VTable layout (after IUnknown):
 * 3: get_StartIndex
 * 4: get_Length
 * 5: get_CorrectiveAction
 * 6: get_Replacement
 */
class ISpellingError(pvInstance: Pointer?) : Unknown(pvInstance) {

    /**
     * Gets the start index of the error in the checked text.
     */
    fun getStartIndex(): Int {
        val startIndex = IntByReference()
        val hr = _invokeNativeInt(
            3, // VTable index for get_StartIndex
            arrayOf(pointer, startIndex)
        )
        COMUtils.checkRC(HRESULT(hr))
        return startIndex.value
    }

    /**
     * Gets the length of the erroneous text.
     */
    fun getLength(): Int {
        val length = IntByReference()
        val hr = _invokeNativeInt(
            4, // VTable index for get_Length
            arrayOf(pointer, length)
        )
        COMUtils.checkRC(HRESULT(hr))
        return length.value
    }

    /**
     * Gets the corrective action to take.
     */
    fun getCorrectiveAction(): Int {
        val action = IntByReference()
        val hr = _invokeNativeInt(
            5, // VTable index for get_CorrectiveAction
            arrayOf(pointer, action)
        )
        COMUtils.checkRC(HRESULT(hr))
        return action.value
    }

    /**
     * Gets the replacement text (for CORRECTIVE_ACTION_REPLACE).
     */
    fun getReplacement(): String? {
        val ppReplacement = PointerByReference()
        val hr = _invokeNativeInt(
            6, // VTable index for get_Replacement
            arrayOf(pointer, ppReplacement)
        )
        COMUtils.checkRC(HRESULT(hr))
        return ppReplacement.value?.getWideString(0)
    }
}

/**
 * JNA wrapper for IEnumString COM interface.
 *
 * VTable layout (after IUnknown):
 * 3: Next
 * 4: Skip
 * 5: Reset
 * 6: Clone
 */
class IEnumString(pvInstance: Pointer?) : Unknown(pvInstance) {

    /**
     * Gets the next string(s).
     *
     * IEnumString::Next signature: HRESULT Next(ULONG celt, LPOLESTR* rgelt, ULONG* pceltFetched)
     *
     * @param count Number of strings to retrieve
     * @return List of strings retrieved
     */
    fun next(count: Int = 1): List<String> {
        val results = mutableListOf<String>()

        // Allocate memory for the output array of string pointers
        val rgelt = com.sun.jna.Memory(com.sun.jna.Native.POINTER_SIZE.toLong() * count)
        val pceltFetched = IntByReference()

        val hr = _invokeNativeInt(
            3, // VTable index for Next
            arrayOf(pointer, count, rgelt, pceltFetched)
        )

        // S_OK (0) or S_FALSE (1) are both valid
        if (hr == 0 || hr == 1) {
            val fetched = pceltFetched.value
            for (i in 0 until fetched) {
                val strPtr = rgelt.getPointer(i.toLong() * com.sun.jna.Native.POINTER_SIZE)
                if (strPtr != null) {
                    val str = strPtr.getWideString(0)
                    if (str != null) {
                        results.add(str)
                    }
                }
            }
        }

        return results
    }

    /**
     * Collects all strings into a list.
     */
    fun toList(): List<String> {
        val strings = mutableListOf<String>()
        var batch = next(1)
        while (batch.isNotEmpty()) {
            strings.addAll(batch)
            batch = next(1)
        }
        return strings
    }
}
