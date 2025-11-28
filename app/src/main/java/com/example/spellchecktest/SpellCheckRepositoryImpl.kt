package com.example.spellchecktest

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SpellCheckRepositoryImpl(private val context: Context) : SpellCheckRepository {

    private val textServicesManager = context.getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager

    override suspend fun performSpellCheck(text: String): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        return suspendCancellableCoroutine { continuation ->
            val spellCheckerSession = textServicesManager.newSpellCheckerSession(
                null,
                null,
                object : SpellCheckerSession.SpellCheckerSessionListener {
                    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
                        val suggestions = mutableListOf<String>()

                        results?.forEach { suggestionsInfo ->
                            if (suggestionsInfo.suggestionsCount > 0) {
                                for (i in 0 until suggestionsInfo.suggestionsCount) {
                                    suggestions.add(suggestionsInfo.getSuggestionAt(i))
                                }
                            }
                        }

                        if (suggestions.isEmpty()) {
                            suggestions.add("No spelling errors found")
                        }

                        continuation.resume(suggestions)
                    }

                    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
                        val suggestions = mutableListOf<String>()

                        results?.forEach { sentenceSuggestionsInfo ->
                            val suggestionsCount = sentenceSuggestionsInfo.suggestionsCount
                            for (i in 0 until suggestionsCount) {
                                val suggestionsInfo = sentenceSuggestionsInfo.getSuggestionsInfoAt(i)
                                if (suggestionsInfo != null && suggestionsInfo.suggestionsCount > 0) {
                                    val offset = sentenceSuggestionsInfo.getOffsetAt(i)
                                    val length = sentenceSuggestionsInfo.getLengthAt(i)
                                    val misspelledWord = text.substring(offset, offset + length)

                                    for (j in 0 until suggestionsInfo.suggestionsCount) {
                                        val suggestion = suggestionsInfo.getSuggestionAt(j)
                                        suggestions.add("'$misspelledWord' → '$suggestion'")
                                    }
                                }
                            }
                        }

                        if (suggestions.isEmpty()) {
                            suggestions.add("No spelling errors found")
                        }

                        continuation.resume(suggestions)
                    }
                },
                true
            )

            if (spellCheckerSession == null) {
                continuation.resume(listOf("Failed to create spell checker session"))
                return@suspendCancellableCoroutine
            }

            // Use getSentenceSuggestions for better context-aware spell checking
            spellCheckerSession.getSentenceSuggestions(
                arrayOf(TextInfo(text)),
                5 // Max suggestions per word
            )

            // Clean up session when coroutine is cancelled
            continuation.invokeOnCancellation {
                spellCheckerSession.close()
            }
        }
    }
}
