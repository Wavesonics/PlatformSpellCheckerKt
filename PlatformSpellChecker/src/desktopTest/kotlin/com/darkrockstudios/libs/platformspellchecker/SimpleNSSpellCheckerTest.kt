package com.darkrockstudios.libs.platformspellchecker

import com.darkrockstudios.libs.platformspellchecker.macos.NSSpellCheckerWrapper
import com.darkrockstudios.libs.platformspellchecker.macos.ObjC
import com.darkrockstudios.libs.platformspellchecker.native.NativeSpellCheckerFactory
import com.darkrockstudios.libs.platformspellchecker.native.OperatingSystem
import com.sun.jna.Pointer
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test

/**
 * Simple test to understand how NSSpellChecker works.
 */
class SimpleNSSpellCheckerTest {

    @Before
    fun setup() {
        // Skip tests if not running on macOS
        val isMacOS = NativeSpellCheckerFactory.currentOS == OperatingSystem.MACOS
        assumeTrue("Not running on macOS", isMacOS)
    }

    @Test
    fun `test guessesForWord behavior`() {
        val wrapper = NSSpellCheckerWrapper.shared()
        kotlin.test.assertNotNull(wrapper, "Wrapper should not be null")

        try {
            // Test correct word
            println("\n=== Testing 'hello' (should be correct) ===")
            testWord(wrapper!!, "hello")

            // Test misspelled word
            println("\n=== Testing 'helllo' (should be misspelled) ===")
            testWord(wrapper, "helllo")

            // Test nonsense word
            println("\n=== Testing 'xyzabc' (should be misspelled) ===")
            testWord(wrapper, "xyzabc")

            // Test another correct word
            println("\n=== Testing 'test' (should be correct) ===")
            testWord(wrapper, "test")

        } finally {
            wrapper?.close()
        }
    }

    private fun testWord(wrapper: NSSpellCheckerWrapper, word: String) {
        val nsWord = ObjC.createNSString(word)
        if (nsWord == null) {
            println("Failed to create NSString for '$word'")
            return
        }

        try {
            val spellCheckerClass = ObjC.getClass("NSSpellChecker")!!
            val sharedSelector = ObjC.selector("sharedSpellChecker")!!
            val spellChecker = ObjC.msgSend(spellCheckerClass, sharedSelector)!!

            val selector = ObjC.selector("guessesForWord:")
            if (selector == null) {
                println("Failed to get guessesForWord: selector")
                return
            }

            val resultArray = ObjC.msgSend(spellChecker, selector, nsWord)
            println("resultArray pointer: $resultArray")

            if (resultArray == null) {
                println("guessesForWord: returned null")
            } else {
                val countSelector = ObjC.selector("count")!!
                val countResult = ObjC.msgSend(resultArray, countSelector)
                println("countResult pointer: $countResult")

                val count = Pointer.nativeValue(countResult)
                println("count value: $count")

                if (count > 0) {
                    println("Suggestions found ($count):")
                    val objectAtIndexSelector = ObjC.selector("objectAtIndex:")!!
                    for (i in 0 until minOf(count, 5)) {
                        val nsString = ObjC.msgSend(resultArray, objectAtIndexSelector, i)
                        val string = ObjC.nsStringToString(nsString)
                        println("  - $string")
                    }
                } else {
                    println("No suggestions found")
                }
            }

            // Also test the wrapper method
            val suggestions = wrapper.getSuggestions(word)
            println("wrapper.getSuggestions returned: $suggestions")

            val isCorrect = wrapper.isWordCorrect(word)
            println("wrapper.isWordCorrect returned: $isCorrect")

        } finally {
            ObjC.release(nsWord)
        }
    }
}
