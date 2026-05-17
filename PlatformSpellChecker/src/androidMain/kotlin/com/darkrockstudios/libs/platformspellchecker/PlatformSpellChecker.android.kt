package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import android.os.Build
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import io.github.aakira.napier.Napier
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val DEFAULT_OPERATION_TIMEOUT: Duration = 3.seconds

actual class PlatformSpellChecker(
	context: Context,
	private val locale: SpLocale? = null,
	private val operationTimeout: Duration = DEFAULT_OPERATION_TIMEOUT,
) : AutoCloseable {
	// Application context so the main-thread Executor handed to the API 33+
	// session overload doesn't pin an Activity.
	private val appContext = context.applicationContext ?: context

	private val textServicesManager =
		appContext.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager

	private val sequenceGenerator = AtomicInteger(0)

	private val pendingOperations = ConcurrentHashMap<Int, OperationContext>()

	private val userDict = UserDictionary()

	private val spellCheckerSession: SpellCheckerSession by lazy {
		val targetLocale = locale?.toJavaLocale() ?: java.util.Locale.getDefault()

		val session = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			// Locale, supportedAttributes bitmask, and main-Executor delivery match
			// the AOSP framework's own usage in frameworks/base/core/java/android/
			// widget/SpellChecker.java.
			val supportedAttributes = SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY or
					SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_TYPO or
					SuggestionsInfo.RESULT_ATTR_LOOKS_LIKE_GRAMMAR_ERROR
			val params = SpellCheckerSession.SpellCheckerSessionParams.Builder()
				.setLocale(targetLocale)
				.setSupportedAttributes(supportedAttributes)
				.build()
			textServicesManager.newSpellCheckerSession(
				params,
				appContext.mainExecutor,
				spellCheckerSessionListener
			)
		} else {
			@Suppress("DEPRECATION")
			textServicesManager.newSpellCheckerSession(
				null,
				targetLocale,
				spellCheckerSessionListener,
				false
			)
		}
		session ?: error("No spell-checker provider available for locale=$targetLocale")
	}

	private sealed class OperationContext {
		data class WordCheck(
			val continuation: Continuation<WordCheckResult>,
			val text: String,
			val max: Int,
		) : OperationContext()

		data class SentenceCheck(
			val continuation: Continuation<List<SpellingCorrection>>,
			val text: String,
		) : OperationContext()

		data class WordIsCorrect(
			val continuation: Continuation<Boolean>,
			val text: String,
		) : OperationContext()
	}

	private val spellCheckerSessionListener = object : SpellCheckerSession.SpellCheckerSessionListener {
		override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
			val cookie = results?.firstOrNull()?.cookie ?: return
			val context = pendingOperations.remove(cookie) ?: return

			val inDict = isWordCorrectFromResults(results)
			when (context) {
				is OperationContext.WordCheck ->
					if (inDict) context.continuation.resume(CorrectWord(context.text))
					else context.continuation.resume(MisspelledWord(context.text, extractSuggestions(results, context.max)))
				is OperationContext.WordIsCorrect ->
					context.continuation.resume(inDict)
				is OperationContext.SentenceCheck ->
					context.continuation.resume(emptyList())
			}
		}

		override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
			val firstResult = results?.firstOrNull() ?: return
			val cookie = firstResult.getSuggestionsInfoAt(0)?.cookie ?: return
			val context = pendingOperations.remove(cookie) ?: return

			when (context) {
				is OperationContext.SentenceCheck ->
					context.continuation.resume(processSentenceSuggestions(results, context.text))
				is OperationContext.WordCheck ->
					context.continuation.resume(CorrectWord(context.text))
				is OperationContext.WordIsCorrect ->
					context.continuation.resume(true)
			}
		}
	}

	actual suspend fun checkMultiword(text: String): List<SpellingCorrection> {
		if (text.isBlank()) return emptyList()
		val raw = withTimeoutFailingOpen("checkMultiword", text, emptyList<SpellingCorrection>()) {
			suspendCancellableCoroutine { continuation ->
				val trackingId = sequenceGenerator.incrementAndGet()
				pendingOperations[trackingId] = OperationContext.SentenceCheck(continuation, text)

				spellCheckerSession.getSentenceSuggestions(
					arrayOf(TextInfo(text, trackingId, 0)),
					5
				)

				continuation.invokeOnCancellation { pendingOperations.remove(trackingId) }
			}
		}
		return raw.filterNot { userDict.isKnown(it.misspelledWord) }
	}

	actual suspend fun checkWord(word: String, maxSuggestions: Int): WordCheckResult {
		val trimmedWord = word.trim()
		if (trimmedWord.isEmpty() || trimmedWord.contains(" ")) return CorrectWord(trimmedWord)
		if (userDict.isKnown(trimmedWord)) return CorrectWord(trimmedWord)
		val max = if (maxSuggestions <= 0) 5 else maxSuggestions

		return withTimeoutFailingOpen<WordCheckResult>("checkWord", trimmedWord, CorrectWord(trimmedWord)) {
			suspendCancellableCoroutine { continuation ->
				val trackingId = sequenceGenerator.incrementAndGet()
				pendingOperations[trackingId] = OperationContext.WordCheck(continuation, trimmedWord, max)

				spellCheckerSession.getSuggestions(
					TextInfo(trimmedWord, trackingId, 0),
					max
				)

				continuation.invokeOnCancellation { pendingOperations.remove(trackingId) }
			}
		}
	}

	actual suspend fun isWordCorrect(word: String): Boolean {
		val trimmed = word.trim()
		if (trimmed.isEmpty() || trimmed.contains(" ")) return false
		if (userDict.isKnown(trimmed)) return true

		return withTimeoutFailingOpen("isWordCorrect", trimmed, true) {
			suspendCancellableCoroutine { continuation ->
				val trackingId = sequenceGenerator.incrementAndGet()
				pendingOperations[trackingId] = OperationContext.WordIsCorrect(continuation, trimmed)

				spellCheckerSession.getSuggestions(TextInfo(trimmed, trackingId, 0), 1)

				continuation.invokeOnCancellation { pendingOperations.remove(trackingId) }
			}
		}
	}

	actual suspend fun addToDictionary(word: String, scope: DictionaryScope) {
		// SpellCheckerSession has no native add/learn API and the system
		// UserDictionary provider requires the signature-level
		// WRITE_USER_DICTIONARY permission as of API 23, so System falls back
		// to app-local persistence regardless of caller intent.
		if (scope == DictionaryScope.System) {
			Napier.w("DictionaryScope.System is not supported on Android; falling back to AppLocal")
		}
		userDict.add(word)
	}

	actual suspend fun removeFromDictionary(word: String, scope: DictionaryScope) {
		if (scope == DictionaryScope.System) {
			Napier.w("DictionaryScope.System is not supported on Android; falling back to AppLocal")
		}
		userDict.remove(word)
	}

	actual suspend fun ignoreWord(word: String) {
		userDict.ignore(word)
	}

	actual fun userDictionary(): Set<String> = userDict.snapshot()

	// SpellCheckerSession dispatches requests over an async IPC bind to the
	// provider app. If the bind never completes — e.g. the provider's process
	// is suspended by OEM background restrictions — the callback never fires
	// and the request hangs indefinitely. Cap each call so we degrade
	// gracefully instead.
	private suspend inline fun <T> withTimeoutFailingOpen(
		operation: String,
		input: String,
		fallback: T,
		crossinline block: suspend () -> T,
	): T = try {
		withTimeout(operationTimeout) { block() }
	} catch (_: TimeoutCancellationException) {
		Napier.w("$operation('$input') timed out after $operationTimeout — spell-checker provider did not respond")
		fallback
	}

	actual override fun close() {
		runCatching { spellCheckerSession.close() }
	}

	private fun processSentenceSuggestions(
		results: Array<out SentenceSuggestionsInfo>?,
		text: String,
	): List<SpellingCorrection> {
		val corrections = mutableListOf<SpellingCorrection>()
		results?.forEach { sentenceSuggestionsInfo ->
			for (i in 0 until sentenceSuggestionsInfo.suggestionsCount) {
				val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(i) ?: continue
				val offset = sentenceSuggestionsInfo.getOffsetAt(i)
				val length = sentenceSuggestionsInfo.getLengthAt(i)
				val misspelledWord = text.substring(offset, offset + length)

				val wordSuggestions = (0 until suggestionsInfo.suggestionsCount)
					.map { suggestionsInfo.getSuggestionAt(it) }

				corrections.add(
					SpellingCorrection(
						misspelledWord = misspelledWord,
						startIndex = offset,
						length = length,
						suggestions = wordSuggestions
					)
				)
			}
		}
		return corrections
	}

	private fun isWordCorrectFromResults(results: Array<out SuggestionsInfo>?): Boolean {
		val info = results?.firstOrNull() ?: return false
		return (info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0
	}

	private fun extractSuggestions(results: Array<out SuggestionsInfo>?, max: Int): List<String> {
		val info = results?.firstOrNull() ?: return emptyList()
		if ((info.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0) return emptyList()
		return (0 until minOf(info.suggestionsCount, max)).map { info.getSuggestionAt(it) }
	}
}
