package com.example.spellchecktest

interface SpellCheckRepository {
    suspend fun performSpellCheck(text: String): List<String>
}
