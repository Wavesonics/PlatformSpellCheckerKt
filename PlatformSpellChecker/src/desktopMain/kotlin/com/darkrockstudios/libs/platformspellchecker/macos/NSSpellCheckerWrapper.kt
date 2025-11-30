package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.Pointer

class NSSpellCheckerWrapper private constructor(private val spellChecker: Pointer) : AutoCloseable {

	private var spellDocumentTag: Long = 0

	init {
		spellDocumentTag = System.currentTimeMillis()
	}

	fun checkSpelling(text: String, startingAt: Long = 0): NSRange {
		val nsString = ObjC.createNSString(text) ?: return NSRange(Long.MAX_VALUE, 0)

		try {
			val selector = ObjC.selector("checkSpellingOfString:startingAt:")
				?: return NSRange(Long.MAX_VALUE, 0)

			return MacOSSpellCheckerLib.INSTANCE.objc_msgSend(
				spellChecker,
				selector,
				nsString,
				startingAt
			)
		} finally {
			ObjC.release(nsString)
		}
	}

	/**
	 * Gets suggestions for a specific word.
	 */
	fun getSuggestions(word: String, language: String? = null): List<String> {
		val nsWord = ObjC.createNSString(word) ?: return emptyList()
		val nsLanguage = language?.let { ObjC.createNSString(it) }

		try {
			val selector = ObjC.selector("guessesForWordRange:inString:language:inSpellDocumentWithTag:")
				?: return emptyList()

			// Create range covering the whole word
			val range = NSRange(0, word.length.toLong())

			val resultArrayPtr = MacOSSpellCheckerLib.INSTANCE.objc_msgSend(
				spellChecker,
				selector,
				range,
				nsWord,
				nsLanguage,
				spellDocumentTag
			)

			if (resultArrayPtr == null || Pointer.nativeValue(resultArrayPtr) == 0L) {
				return emptyList()
			}

			return nsArrayToStringList(resultArrayPtr)
		} finally {
			ObjC.release(nsWord)
			nsLanguage?.let { ObjC.release(it) }
		}
	}

	/**
	 * Checks if a word is spelled correctly.
	 */
	fun isWordCorrect(word: String): Boolean {
		// If checkSpelling returns NSNotFound (MAX_VALUE), the word is correct.
		val range = checkSpelling(word, 0)
		return range.location == Long.MAX_VALUE
	}

	/**
	 * Adds a word to the user dictionary.
	 */
	fun learnWord(word: String) {
		val nsWord = ObjC.createNSString(word) ?: return
		try {
			val selector = ObjC.selector("learnWord:") ?: return
			ObjC.msgSend(spellChecker, selector, nsWord)
		} finally {
			ObjC.release(nsWord)
		}
	}

	/**
	 * Ignores a word for this session.
	 */
	fun ignoreWord(word: String) {
		val nsWord = ObjC.createNSString(word) ?: return
		try {
			val selector = ObjC.selector("ignoreWord:inSpellDocumentWithTag:") ?: return
			ObjC.msgSend(spellChecker, selector, nsWord, spellDocumentTag, 0)
		} finally {
			ObjC.release(nsWord)
		}
	}

	/**
	 * Sets the language.
	 */
	fun setLanguage(language: String): Boolean {
		val nsLanguage = ObjC.createNSString(language) ?: return false
		try {
			val selector = ObjC.selector("setLanguage:") ?: return false
			val result = ObjC.msgSend(spellChecker, selector, nsLanguage)
			return Pointer.nativeValue(result) != 0L
		} finally {
			ObjC.release(nsLanguage)
		}
	}

	/**
	 * Gets the current language.
	 */
	fun getLanguage(): String? {
		val selector = ObjC.selector("language") ?: return null
		val nsLanguage = ObjC.msgSend(spellChecker, selector) ?: return null
		return ObjC.nsStringToString(nsLanguage)
	}

	override fun close() {
		val closeSelector = ObjC.selector("closeSpellDocumentWithTag:")
		if (closeSelector != null && spellDocumentTag != 0L) {
			ObjC.msgSend(spellChecker, closeSelector, spellDocumentTag, 0)
		}
	}

	companion object {
		/**
		 * Gets the shared NSSpellChecker instance.
		 */
		fun shared(): NSSpellCheckerWrapper? {
			val nsSpellCheckerClass = ObjC.getClass("NSSpellChecker") ?: return null
			val sharedSelector = ObjC.selector("sharedSpellChecker") ?: return null

			val spellChecker = ObjC.msgSend(nsSpellCheckerClass, sharedSelector)
				?: return null

			return NSSpellCheckerWrapper(spellChecker)
		}

		/**
		 * Gets available languages.
		 */
		fun availableLanguages(): List<String> {
			val nsSpellCheckerClass = ObjC.getClass("NSSpellChecker") ?: return emptyList()
			val sharedSelector = ObjC.selector("sharedSpellChecker") ?: return emptyList()
			val spellChecker = ObjC.msgSend(nsSpellCheckerClass, sharedSelector)
				?: return emptyList()

			val languagesSelector = ObjC.selector("availableLanguages") ?: return emptyList()
			val languagesArray = ObjC.msgSend(spellChecker, languagesSelector)
				?: return emptyList()

			return nsArrayToStringList(languagesArray)
		}

		fun isLanguageSupported(languageTag: String): Boolean {
			val available = availableLanguages()
			if (available.contains(languageTag)) return true

			val normalized = languageTag.replace("-", "_")
			if (available.contains(normalized)) return true

			val languagePart = languageTag.split("-", "_").firstOrNull()
			if (languagePart != null && available.any { it.startsWith(languagePart) }) {
				return true
			}
			return false
		}

		private fun nsArrayToStringList(nsArray: Pointer): List<String> {
			val countSelector = ObjC.selector("count") ?: return emptyList()
			val countResult = ObjC.msgSend(nsArray, countSelector) ?: return emptyList()
			val count = Pointer.nativeValue(countResult)

			if (count == 0L) return emptyList()

			val results = ArrayList<String>(count.toInt())
			val objectAtIndex = ObjC.selector("objectAtIndex:") ?: return emptyList()

			for (i in 0 until count) {
				val nsStr = ObjC.msgSend(nsArray, objectAtIndex, i)
				if (nsStr != null) {
					ObjC.nsStringToString(nsStr)?.let { results.add(it) }
				}
			}
			return results
		}
	}
}