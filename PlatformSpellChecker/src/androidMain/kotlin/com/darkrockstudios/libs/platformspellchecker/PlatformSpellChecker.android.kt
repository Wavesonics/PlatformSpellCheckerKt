package com.darkrockstudios.libs.platformspellchecker

import android.content.Context
import android.os.Build
import android.os.LocaleList
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

actual class PlatformSpellChecker(
    context: Context,
    private val locale: SpLocale? = null
) {
    private val textServicesManager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager

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
            val continuation: Continuation<List<String>>,
            val text: String
        ) : OperationContext()

        data class SentenceCheck(
            val continuation: Continuation<List<SpellingCorrection>>,
            val text: String
        ) : OperationContext()
    }

    private val spellCheckerSessionListener = object : SpellCheckerSession.SpellCheckerSessionListener {
        override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
            // Extract cookie (our tracking ID) from the first result
            val cookie = results?.firstOrNull()?.cookie ?: return
            val context = pendingOperations.remove(cookie) ?: return

            when (context) {
                is OperationContext.WordCheck -> {
                    val suggestions = processWordSuggestions(results, context.text)
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
                    context.continuation.resume(emptyList())
                }
            }
        }
    }

    actual suspend fun performSpellCheck(text: String): List<SpellingCorrection> {
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

    actual suspend fun checkWord(word: String): List<String> {
        if (word.isBlank()) {
            return emptyList()
        }

        val trimmedWord = word.trim()
        if (trimmedWord.contains(" ")) {
            return listOf("Please provide a single word only")
        }

        return suspendCancellableCoroutine { continuation ->
            val trackingId = sequenceGenerator.incrementAndGet()
            val context = OperationContext.WordCheck(continuation, trimmedWord)
            pendingOperations[trackingId] = context

            // Use cookie parameter to track this request
            spellCheckerSession.getSuggestions(
                TextInfo(trimmedWord, trackingId, 0),
                5 // Max suggestions
            )

            continuation.invokeOnCancellation {
                pendingOperations.remove(trackingId)
            }
        }
    }

    // Helper function to process word suggestions with dictionary check
    private fun processWordSuggestions(results: Array<out SuggestionsInfo>?, word: String): List<String> {
        val suggestions = mutableListOf<String>()

        results?.firstOrNull()?.let { suggestionsInfo ->
            val isTypo = (suggestionsInfo.suggestionsAttributes and SuggestionsInfo.RESULT_ATTR_IN_THE_DICTIONARY) == 0

            if (isTypo && suggestionsInfo.suggestionsCount > 0) {
                for (i in 0 until suggestionsInfo.suggestionsCount) {
                    suggestions.add(suggestionsInfo.getSuggestionAt(i))
                }
            } else if (!isTypo) {
                suggestions.add("'$word' is correctly spelled")
            } else {
                suggestions.add("'$word' may be misspelled (no suggestions available)")
            }
        }

        if (suggestions.isEmpty()) {
            suggestions.add("Unable to check word")
        }

        return suggestions
    }

    private fun processSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?, text: String): List<SpellingCorrection> {
        val corrections = mutableListOf<SpellingCorrection>()

        results?.forEach { sentenceSuggestionsInfo ->
            val suggestionsCount = sentenceSuggestionsInfo.suggestionsCount
            for (i in 0 until suggestionsCount) {
                val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(i)
                if (suggestionsInfo != null && suggestionsInfo.suggestionsCount > 0) {
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
}
