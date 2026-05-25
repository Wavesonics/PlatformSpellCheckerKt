package com.darkrockstudios.libs.platformspellchecker

import kotlin.concurrent.Volatile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Internal helper shared by every platform's [PlatformSpellChecker] actual to
 * track app-local additions and per-session ignores. Reads use volatile
 * snapshots so the spell-check hot path stays lock-free; mutations are
 * serialized under a coroutine [Mutex].
 *
 * Matching is case-insensitive and trims whitespace, matching the convention
 * used by [com.darkrockstudios.libs.platformspellchecker.linux.HunspellWrapper]
 * and the macOS wrapper.
 */
internal class UserDictionary {
	private val mutex = Mutex()

	@Volatile
	private var addedWords: Set<String> = emptySet()

	@Volatile
	private var ignoredWords: Set<String> = emptySet()

	suspend fun add(word: String) {
		val key = word.normalize() ?: return
		mutex.withLock { addedWords = addedWords + key }
	}

	suspend fun remove(word: String) {
		val key = word.normalize() ?: return
		mutex.withLock { addedWords = addedWords - key }
	}

	suspend fun ignore(word: String) {
		val key = word.normalize() ?: return
		mutex.withLock { ignoredWords = ignoredWords + key }
	}

	// Returns a defensive copy: the volatile `addedWords` is the LinkedHashSet
	// returned by Set.plus, and handing it out directly lets a caller downcast
	// to MutableSet and corrupt internal state until the next mutation
	// reassigns the reference.
	fun snapshot(): Set<String> = addedWords.toSet()

	/** True when [word] should be treated as correctly spelled by the caller. */
	fun isKnown(word: String): Boolean {
		val key = word.normalize() ?: return false
		return addedWords.contains(key) || ignoredWords.contains(key)
	}

	private fun String.normalize(): String? = trim().lowercase().takeIf { it.isNotEmpty() }
}
