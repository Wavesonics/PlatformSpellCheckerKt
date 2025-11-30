package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import android.view.textservice.*
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

actual class PlatformSpellChecker(
	context: Context,
	private val locale: SpLocale? = null
) : AutoCloseable {
	private val textServicesManager =
		context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager

	private val sequenceGenerator = AtomicInteger(0)

	private val pendingOperations = ConcurrentHashMap<Int, OperationContext>()

	private val spellCheckerSession: SpellCheckerSession by lazy {
		// If a locale was provided, build a java.util.Locale and do not refer to spell checker language settings
		val referToSettings = locale == null
		val targetLocale = locale?.let {
			if (it.country.isNullOrBlank()) java.util.Locale(it.language)
			else java.util.Locale(it.language, it.country)
		}
		textServicesManager.newSpellCheckerSession(
			null,
			targetLocale,
			spellCheckerSessionListener,
			referToSettings
		)!!
	}

	private sealed class OperationContext {
		data class WordCheck(
			val continuation: Continuation<WordCheckResult>,
			val text: String,
			val max: Int
		) : OperationContext()

		data class SentenceCheck(
			val continuation: Continuation<List<SpellingCorrection>>,
			val text: String
		) : OperationContext()

		data class WordIsCorrect(
			val continuation: Continuation<Boolean>,
			val text: String
		) : OperationContext()

		data class Suggestions(
			val continuation: Continuation<List<String>>,
			val text: String,
			val max: Int
		) : OperationContext()
	}

	private val spellCheckerSessionListener = object : SpellCheckerSession.SpellCheckerSessionListener {
		override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
			// Extract cookie (our tracking ID) from the first result
			val cookie = results?.firstOrNull()?.cookie ?: return
			val context = pendingOperations.remove(cookie) ?: return

			when (context) {
				is OperationContext.WordCheck -> {
					val inDict = isWordCorrectFromResults(results)
					if (inDict) {
						context.continuation.resume(CorrectWord(context.text))
					} else {
						val suggestions = extractSuggestions(results, context.max)
						context.continuation.resume(MisspelledWord(context.text, suggestions))
					}
				}

				is OperationContext.WordIsCorrect -> {
					val isCorrect = isWordCorrectFromResults(results)
					context.continuation.resume(isCorrect)
				}

				is OperationContext.Suggestions -> {
					val suggestions = extractSuggestions(results, context.max)
					context.continuation.resume(suggestions)
				}

				is OperationContext.SentenceCheck -> {
					// Fallback for simple suggestions (shouldn't normally be called for sentence check)
					context.continuation.resume(emptyList())
				}
			}
		}

		override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
			val firstResult = results?.firstOrNull() ?: return
			// Extract cookie from the first SuggestionsInfo in the sentence result
			val cookie = firstResult.getSuggestionsInfoAt(0)?.cookie ?: return
			val context = pendingOperations.remove(cookie) ?: return

			when (context) {
				is OperationContext.SentenceCheck -> {
					val corrections = processSentenceSuggestions(results, context.text)
					context.continuation.resume(corrections)
				}

				is OperationContext.WordCheck -> {
					// Shouldn't happen, but handle gracefully
					context.continuation.resume(CorrectWord(context.text))
				}

				is OperationContext.WordIsCorrect -> {
					// Shouldn't happen for sentence suggestions
					context.continuation.resume(false)
				}

				is OperationContext.Suggestions -> {
					// Shouldn't happen for sentence suggestions
					context.continuation.resume(emptyList())
				}
			}
		}
	}

	actual suspend fun checkMultiword(text: String): List<SpellingCorrection> {
		if (text.isBlank()) {
			return emptyList()
		}

		return suspendCancellableCoroutine { continuation ->
			val trackingId = sequenceGenerator.incrementAndGet()
			val context = OperationContext.SentenceCheck(continuation, text)
			pendingOperations[trackingId] = context

			// Use cookie parameter to track this request
			spellCheckerSession.getSentenceSuggestions(
				arrayOf(TextInfo(text, trackingId, 0)),
				5 // Max suggestions per word
			)

			continuation.invokeOnCancellation {
				pendingOperations.remove(trackingId)
			}
		}
	}

	actual suspend fun checkWord(word: String, maxSuggestions: Int): WordCheckResult {
		val trimmedWord = word.trim()
		if (trimmedWord.isEmpty() || trimmedWord.contains(" ")) return CorrectWord(trimmedWord)
		val max = if (maxSuggestions <= 0) 5 else maxSuggestions

		return suspendCancellableCoroutine { continuation ->
			val trackingId = sequenceGenerator.incrementAndGet()
			val context = OperationContext.WordCheck(continuation, trimmedWord, max)
			pendingOperations[trackingId] = context

			// Use cookie parameter to track this request
			spellCheckerSession.getSuggestions(
				TextInfo(trimmedWord, trackingId, 0),
				max
			)

			continuation.invokeOnCancellation {
				pendingOperations.remove(trackingId)
			}
		}
	}

	actual suspend fun isWordCorrect(word: String): Boolean {
		val trimmed = word.trim()
		if (trimmed.isEmpty() || trimmed.contains(" ")) return false

		return suspendCancellableCoroutine { continuation ->
			val trackingId = sequenceGenerator.incrementAndGet()
			val context = OperationContext.WordIsCorrect(continuation, trimmed)
			pendingOperations[trackingId] = context

			spellCheckerSession.getSuggestions(
				TextInfo(trimmed, trackingId, 0),
				1
			)

			continuation.invokeOnCancellation {
				pendingOperations.remove(trackingId)
			}
		}
	}

	actual override fun close() {
		runCatching { spellCheckerSession.close() }
	}

	private fun processSentenceSuggestions(
		results: Array<out SentenceSuggestionsInfo>?,
		text: String
	): List<SpellingCorrection> {
		val corrections = mutableListOf<SpellingCorrection>()

		results?.forEach { sentenceSuggestionsInfo ->
			val suggestionsCount = sentenceSuggestionsInfo.suggestionsCount
			for (i in 0 until suggestionsCount) {
				val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(i)
				if (suggestionsInfo != null) {
					val offset = sentenceSuggestionsInfo.getOffsetAt(i)
					val length = sentenceSuggestionsInfo.getLengthAt(i)
					val misspelledWord = text.substring(offset, offset + length)

					val wordSuggestions = mutableListOf<String>()
					for (j in 0 until suggestionsInfo.suggestionsCount) {
						wordSuggestions.add(suggestionsInfo.getSuggestionAt(j))
					}

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
		}

		return corrections
	}

	private fun isWordCorrectFromResults(results: Array<out SuggestionsInfo>?): Boolean {
		val suggestionsInfo = results?.firstOrNull() ?: return false
		val inDict = (suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0
		return inDict
	}

	private fun extractSuggestions(results: Array<out SuggestionsInfo>?, max: Int): List<String> {
		val out = mutableListOf<String>()
		val suggestionsInfo = results?.firstOrNull() ?: return emptyList()
		val inDict = (suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) != 0
		if (inDict) return emptyList()

		val count = minOf(suggestionsInfo.suggestionsCount, max)
		for (i in 0 until count) {
			out.add(suggestionsInfo.getSuggestionAt(i))
		}
		return out
	}
}
