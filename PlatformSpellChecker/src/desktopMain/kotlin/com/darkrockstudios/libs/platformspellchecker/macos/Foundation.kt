package com.darkrockstudios.libs.platformspellchecker.macos

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure

/**
 * JNA bindings for macOS Objective-C runtime and Foundation framework.
 *
 * This provides low-level access to:
 * - Objective-C runtime (objc_* functions)
 * - Core Foundation string functions
 * - NSObject methods
 *
 * Reference: https://developer.apple.com/documentation/objectivec/objective-c_runtime
 */
interface Foundation : Library {
	companion object {
		val INSTANCE: Foundation by lazy {
			Native.load("Foundation", Foundation::class.java) as Foundation
		}

		// Load the Objective-C runtime library for additional functions
		val OBJC_RUNTIME: ObjCRuntime by lazy {
			Native.load("objc", ObjCRuntime::class.java) as ObjCRuntime
		}
	}

	// Objective-C Runtime Functions

	/**
	 * Gets a class by name.
	 * @param className The name of the class (e.g., "NSSpellChecker")
	 * @return Pointer to the class, or null if not found
	 */
	fun objc_getClass(className: String): Pointer?

	/**
	 * Registers a selector (method name) and returns a pointer to it.
	 * @param selectorName The selector name (e.g., "sharedSpellChecker")
	 * @return Pointer to the selector
	 */
	fun sel_registerName(selectorName: String): Pointer?

	/**
	 * Retains an Objective-C object (increments reference count).
	 */
	fun objc_retain(obj: Pointer?): Pointer?

	/**
	 * Releases an Objective-C object (decrements reference count).
	 */
	fun objc_release(obj: Pointer?)

	// NSString Conversion Functions

	/**
	 * Creates an NSString from a UTF-8 C string.
	 * Equivalent to: [[NSString alloc] initWithUTF8String:cString]
	 */
	fun CFStringCreateWithCString(
		alloc: Pointer?,
		cString: String,
		encoding: Int
	): Pointer?

	/**
	 * Gets the length of an NSString.
	 */
	fun CFStringGetLength(string: Pointer?): Long

	/**
	 * Gets characters from an NSString.
	 */
	fun CFStringGetCharacters(
		string: Pointer?,
		range: Pointer?,
		buffer: CharArray
	)

	/**
	 * Creates a Java String from an NSString pointer.
	 */
	fun CFStringGetCStringPtr(
		string: Pointer?,
		encoding: Int
	): Pointer?
}

/**
 * JNA interface for Objective-C runtime functions.
 * These are in the libobjc dynamic library.
 */
interface ObjCRuntime : Library {
	/**
	 * Sends a message to an Objective-C object (no arguments).
	 * Returns a pointer (for object returns).
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?): Pointer?

	/**
	 * Sends a message with one pointer argument.
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?): Pointer?

	/**
	 * Sends a message with two pointer arguments.
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Pointer?): Pointer?

	/**
	 * Sends a message with one long argument.
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Long): Pointer?

	/**
	 * Sends a message with two long arguments.
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Long, arg2: Long): Pointer?

	/**
	 * Sends a message with mixed arguments (Pointer, Long).
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Long): Pointer?

	/**
	 * Sends a message with mixed arguments (Pointer, Long, Long).
	 */
	fun objc_msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Long, arg3: Long): Pointer?
}

/**
 * Helper object for working with Objective-C objects and strings.
 */
object ObjC {
	private val foundation = Foundation.INSTANCE
	private val objc = Foundation.OBJC_RUNTIME

	// UTF-8 encoding constant
	const val NSUTF8StringEncoding = 4

	// NSRange structure offsets
	const val NSNotFound = Long.MAX_VALUE

	/**
	 * Gets a class by name.
	 */
	fun getClass(name: String): Pointer? = foundation.objc_getClass(name)

	/**
	 * Registers a selector.
	 */
	fun selector(name: String): Pointer? = foundation.sel_registerName(name)

	/**
	 * Sends a message with no arguments.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?): Pointer? =
		objc.objc_msgSend(receiver, selector)

	/**
	 * Sends a message with one pointer argument.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1)

	/**
	 * Sends a message with two pointer arguments.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Pointer?): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1, arg2)

	/**
	 * Sends a message with one long argument.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Long): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1)

	/**
	 * Sends a message with integer arguments.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Long, arg2: Long): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1, arg2)

	/**
	 * Sends a message with mixed arguments (Pointer, Long).
	 * Useful for methods that take an object and an integer/index.
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Long): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1, arg2)

	/**
	 * Sends a message with mixed arguments (Pointer, Long, Long).
	 */
	fun msgSend(receiver: Pointer?, selector: Pointer?, arg1: Pointer?, arg2: Long, arg3: Long): Pointer? =
		objc.objc_msgSend(receiver, selector, arg1, arg2, arg3)

	/**
	 * Retains an Objective-C object.
	 */
	fun retain(obj: Pointer?): Pointer? = foundation.objc_retain(obj)

	/**
	 * Releases an Objective-C object.
	 */
	fun release(obj: Pointer?) = foundation.objc_release(obj)

	/**
	 * Creates an NSString from a Kotlin String.
	 */
	fun createNSString(string: String): Pointer? {
		val nsStringClass = getClass("NSString") ?: return null
		val allocSelector = selector("alloc") ?: return null
		val initSelector = selector("initWithUTF8String:") ?: return null

		// [NSString alloc]
		val nsString = msgSend(nsStringClass, allocSelector) ?: return null

		// [nsString initWithUTF8String:string]
		// Create a C string pointer for the string
		val cString = com.sun.jna.Memory((string.length + 1).toLong())
		cString.setString(0, string)
		return msgSend(nsString, initSelector, cString)
	}

	/**
	 * Converts an NSString to a Kotlin String.
	 */
	fun nsStringToString(nsString: Pointer?): String? {
		if (nsString == null) return null

		val utf8Selector = selector("UTF8String") ?: return null
		val cStringPtr = msgSend(nsString, utf8Selector) ?: return null

		return cStringPtr.getString(0, "UTF-8")
	}

	/**
	 * Creates an NSRange structure (location, length).
	 */
	fun createNSRange(location: Long, length: Long): NSRange {
		return NSRange(location, length)
	}
}

@Structure.FieldOrder("location", "length")
open class NSRange : Structure, Structure.ByValue {
	@JvmField
	var location: Long = 0

	@JvmField
	var length: Long = 0

	constructor()
	constructor(location: Long, length: Long) {
		this.location = location
		this.length = length
	}

	/**
	 * Converts to a Pointer for passing to Objective-C methods.
	 */
	fun toPointer(): Pointer {
		val memory = com.sun.jna.Memory(16) // NSRange is 16 bytes (2 x 8-byte longs)
		memory.setLong(0, location)
		memory.setLong(8, length)
		return memory
	}

	fun isFound(): Boolean = location != Long.MAX_VALUE

	companion object {
		/**
		 * Reads an NSRange from a Pointer.
		 */
		fun fromPointer(ptr: Pointer): NSRange {
			return NSRange(
				location = ptr.getLong(0),
				length = ptr.getLong(8)
			)
		}
	}
}