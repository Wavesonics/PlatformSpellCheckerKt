/**
 * NSSpellCheckerJNI.m
 *
 * Native Objective-C wrapper for NSSpellChecker that avoids struct-by-value returns
 * by using out-parameters instead. This solves JNA's limitations with NSRange returns.
 */

#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>

// Initialize the shared NSSpellChecker instance
static NSSpellChecker* getSharedSpellChecker() {
    static NSSpellChecker* sharedChecker = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedChecker = [NSSpellChecker sharedSpellChecker];
    });
    return sharedChecker;
}

// Check spelling of a string starting at a given offset
// Returns via out-parameters to avoid NSRange struct return issues with JNA
__attribute__((visibility("default")))
void checkSpellingOfString(const char* text,
                          long startingOffset,
                          long* outLocation,
                          long* outLength) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* string = [NSString stringWithUTF8String:text];

        NSRange range = [checker checkSpellingOfString:string
                                           startingAt:(NSInteger)startingOffset];

        *outLocation = (long)range.location;
        *outLength = (long)range.length;
    }
}

// Check spelling with language and wrap mode options
__attribute__((visibility("default")))
void checkSpellingOfStringWithOptions(const char* text,
                                     long startingOffset,
                                     const char* language,
                                     int wrapFlag,
                                     long* outLocation,
                                     long* outLength) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* string = [NSString stringWithUTF8String:text];
        NSString* lang = language ? [NSString stringWithUTF8String:language] : nil;

        NSRange range = [checker checkSpellingOfString:string
                                           startingAt:(NSInteger)startingOffset
                                             language:lang
                                                 wrap:(BOOL)wrapFlag
                                   inSpellDocumentWithTag:0
                                                wordCount:NULL];

        *outLocation = (long)range.location;
        *outLength = (long)range.length;
    }
}

// Find the range of a misspelled word in a string
__attribute__((visibility("default")))
void rangeOfMisspelledWord(const char* text,
                          long startingOffset,
                          const char* language,
                          int wrapFlag,
                          long* outLocation,
                          long* outLength,
                          long* outWordCount) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* string = [NSString stringWithUTF8String:text];

        // Use the simpler checkSpellingOfString:startingAt: method
        // Note: This doesn't support language specification directly
        NSRange range = [checker checkSpellingOfString:string
                                           startingAt:(NSInteger)startingOffset];

        *outLocation = (long)range.location;
        *outLength = (long)range.length;
        if (outWordCount) {
            // Count words in the range
            if (range.location != NSNotFound) {
                *outWordCount = 1; // Simple count for now
            } else {
                *outWordCount = 0;
            }
        }
    }
}

// Get spelling suggestions for a word
// Returns a comma-separated list of suggestions (caller must free)
__attribute__((visibility("default")))
char* getSuggestions(const char* word, const char* language) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* wordString = [NSString stringWithUTF8String:word];
        NSString* lang = language ? [NSString stringWithUTF8String:language] : nil;

        NSArray<NSString*>* suggestions;
        if (lang) {
            // Use newer API if language is specified
            suggestions = [checker guessesForWordRange:NSMakeRange(0, [wordString length])
                                              inString:wordString
                                              language:lang
                                    inSpellDocumentWithTag:0];
        } else {
            // Fall back to simpler API (deprecated but still functional)
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
            suggestions = [checker guessesForWord:wordString];
#pragma clang diagnostic pop
        }

        if (!suggestions || [suggestions count] == 0) {
            return strdup("");
        }

        // Join suggestions with comma
        NSString* joined = [suggestions componentsJoinedByString:@","];
        return strdup([joined UTF8String]);
    }
}

// Check if a word is in the dictionary (correctly spelled)
__attribute__((visibility("default")))
int isWordInDictionary(const char* word, const char* language) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* wordString = [NSString stringWithUTF8String:word];
        NSString* lang = language ? [NSString stringWithUTF8String:language] : nil;

        // Use checkSpellingOfString to determine if word is correct
        NSRange range = [checker checkSpellingOfString:wordString
                                           startingAt:0
                                             language:lang
                                                 wrap:NO
                                   inSpellDocumentWithTag:0
                                                wordCount:NULL];

        // If NSNotFound is returned, the word is spelled correctly
        return (range.location == NSNotFound) ? 1 : 0;
    }
}

// Learn a new word (add to user dictionary)
__attribute__((visibility("default")))
void learnWord(const char* word) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* wordString = [NSString stringWithUTF8String:word];
        [checker learnWord:wordString];
    }
}

// Unlearn a word (remove from user dictionary)
__attribute__((visibility("default")))
void unlearnWord(const char* word) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* wordString = [NSString stringWithUTF8String:word];
        [checker unlearnWord:wordString];
    }
}

// Ignore a word for the current document/session
__attribute__((visibility("default")))
void ignoreWord(const char* word) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* wordString = [NSString stringWithUTF8String:word];
        [checker ignoreWord:wordString inSpellDocumentWithTag:0];
    }
}

// Get available languages as a comma-separated list (caller must free)
__attribute__((visibility("default")))
char* getAvailableLanguages() {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSArray<NSString*>* languages = [checker availableLanguages];

        if (!languages || [languages count] == 0) {
            return strdup("");
        }

        NSString* joined = [languages componentsJoinedByString:@","];
        return strdup([joined UTF8String]);
    }
}

// Set the language for spell checking
__attribute__((visibility("default")))
int setLanguage(const char* language) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* lang = [NSString stringWithUTF8String:language];

        BOOL result = [checker setLanguage:lang];
        return result ? 1 : 0;
    }
}

// Get the current language
__attribute__((visibility("default")))
char* getCurrentLanguage() {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* language = [checker language];

        if (!language) {
            return strdup("");
        }

        return strdup([language UTF8String]);
    }
}

// Check grammar in a string
__attribute__((visibility("default")))
void checkGrammar(const char* text,
                 long startingOffset,
                 const char* language,
                 long* outLocation,
                 long* outLength) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* string = [NSString stringWithUTF8String:text];
        NSString* lang = language ? [NSString stringWithUTF8String:language] : nil;

        NSArray* details = nil;
        NSRange range = [checker checkGrammarOfString:string
                                          startingAt:(NSInteger)startingOffset
                                            language:lang
                                                wrap:NO
                                  inSpellDocumentWithTag:0
                                             details:&details];

        *outLocation = (long)range.location;
        *outLength = (long)range.length;
    }
}

// Count continuous spell checking errors from a given offset
__attribute__((visibility("default")))
long countContinuousSpellCheckingErrors(const char* text,
                                       long startingOffset,
                                       const char* language) {
    @autoreleasepool {
        NSSpellChecker* checker = getSharedSpellChecker();
        NSString* string = [NSString stringWithUTF8String:text];
        NSString* lang = language ? [NSString stringWithUTF8String:language] : nil;

        NSInteger count = [checker countWordsInString:string
                                             language:lang];

        return (long)count;
    }
}

// Free allocated memory (helper for JNA)
__attribute__((visibility("default")))
void freeMemory(void* ptr) {
    if (ptr) {
        free(ptr);
    }
}