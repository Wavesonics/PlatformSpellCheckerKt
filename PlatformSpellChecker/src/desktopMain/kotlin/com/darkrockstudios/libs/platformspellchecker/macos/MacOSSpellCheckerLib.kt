package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface MacOSSpellCheckerLib : Library {
    companion object {
        val INSTANCE: MacOSSpellCheckerLib = Native.load("objc", MacOSSpellCheckerLib::class.java)
    }

    fun objc_msgSend(self: Pointer, selector: Pointer, range: NSRange, string: Pointer, language: Pointer?, tag: Long): Pointer

    fun objc_msgSend(self: Pointer, selector: Pointer, string: Pointer, startingAt: Long): NSRange
}