package com.example.spellchecktest

interface SpellCheckRepository {
    suspend fun performSpellCheck(text: String): List<String>
    suspend fun checkWord(word: String): List<String>
}
