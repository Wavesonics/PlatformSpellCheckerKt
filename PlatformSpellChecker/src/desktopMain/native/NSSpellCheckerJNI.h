/**
 * NSSpellCheckerJNI.h
 *
 * C interface for NSSpellChecker wrapper functions
 * These functions use out-parameters instead of returning structs
 * to avoid JNA limitations with NSRange returns.
 */

#ifndef NSSPELLCHECKER_JNI_H
#define NSSPELLCHECKER_JNI_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Check spelling of a string starting at a given offset.
 * @param text The text to check (UTF-8 encoded)
 * @param startingOffset The offset to start checking from
 * @param outLocation Output: location of the first misspelled word (NSNotFound if none)
 * @param outLength Output: length of the misspelled word
 */
void checkSpellingOfString(const char* text,
                          long startingOffset,
                          long* outLocation,
                          long* outLength);

/**
 * Check spelling with language and wrap options.
 * @param text The text to check (UTF-8 encoded)
 * @param startingOffset The offset to start checking from
 * @param language The language to use for spell checking (can be NULL)
 * @param wrapFlag Whether to wrap around to the beginning when reaching the end
 * @param outLocation Output: location of the first misspelled word
 * @param outLength Output: length of the misspelled word
 */
void checkSpellingOfStringWithOptions(const char* text,
                                     long startingOffset,
                                     const char* language,
                                     int wrapFlag,
                                     long* outLocation,
                                     long* outLength);

/**
 * Find the range of a misspelled word in a string.
 * @param text The text to check (UTF-8 encoded)
 * @param startingOffset The offset to start checking from
 * @param language The language to use (can be NULL)
 * @param wrapFlag Whether to wrap around
 * @param outLocation Output: location of the misspelled word
 * @param outLength Output: length of the misspelled word
 * @param outWordCount Output: word count (can be NULL)
 */
void rangeOfMisspelledWord(const char* text,
                          long startingOffset,
                          const char* language,
                          int wrapFlag,
                          long* outLocation,
                          long* outLength,
                          long* outWordCount);

/**
 * Get spelling suggestions for a word.
 * @param word The word to get suggestions for (UTF-8 encoded)
 * @param language The language to use (can be NULL)
 * @return Comma-separated list of suggestions (caller must free with freeMemory)
 */
char* getSuggestions(const char* word, const char* language);

/**
 * Check if a word is in the dictionary (correctly spelled).
 * @param word The word to check (UTF-8 encoded)
 * @param language The language to use (can be NULL)
 * @return 1 if the word is correct, 0 if misspelled
 */
int isWordInDictionary(const char* word, const char* language);

/**
 * Learn a new word (add to user dictionary).
 * @param word The word to learn (UTF-8 encoded)
 */
void learnWord(const char* word);

/**
 * Unlearn a word (remove from user dictionary).
 * @param word The word to unlearn (UTF-8 encoded)
 */
void unlearnWord(const char* word);

/**
 * Ignore a word for the current document/session.
 * @param word The word to ignore (UTF-8 encoded)
 */
void ignoreWord(const char* word);

/**
 * Get available languages.
 * @return Comma-separated list of language codes (caller must free with freeMemory)
 */
char* getAvailableLanguages();

/**
 * Set the language for spell checking.
 * @param language The language code to set
 * @return 1 if successful, 0 if failed
 */
int setLanguage(const char* language);

/**
 * Get the current language.
 * @return The current language code (caller must free with freeMemory)
 */
char* getCurrentLanguage();

/**
 * Check grammar in a string.
 * @param text The text to check (UTF-8 encoded)
 * @param startingOffset The offset to start checking from
 * @param language The language to use (can be NULL)
 * @param outLocation Output: location of the grammar error
 * @param outLength Output: length of the grammar error
 */
void checkGrammar(const char* text,
                 long startingOffset,
                 const char* language,
                 long* outLocation,
                 long* outLength);

/**
 * Count continuous spell checking errors from a given offset.
 * @param text The text to check (UTF-8 encoded)
 * @param startingOffset The offset to start from
 * @param language The language to use (can be NULL)
 * @return The number of errors found
 */
long countContinuousSpellCheckingErrors(const char* text,
                                       long startingOffset,
                                       const char* language);

/**
 * Free memory allocated by this library.
 * @param ptr The pointer to free
 */
void freeMemory(void* ptr);

#ifdef __cplusplus
}
#endif

#endif // NSSPELLCHECKER_JNI_H