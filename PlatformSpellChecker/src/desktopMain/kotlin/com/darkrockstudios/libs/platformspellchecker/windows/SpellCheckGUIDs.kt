package com.darkrockstudios.libs.platformspellchecker.windows

import com.sun.jna.platform.win32.Guid.CLSID
import com.sun.jna.platform.win32.Guid.IID

/**
 * GUIDs for Windows Spell Checking API COM interfaces.
 * These values are from the Windows SDK spellcheck.h header.
 * Source: https://sources.debian.org/src/rust-winapi/0.3.8-2/src/um/spellcheck.rs/
 */
internal object SpellCheckGUIDs {
	// Class ID for SpellCheckerFactory - used with CoCreateInstance
	val CLSID_SpellCheckerFactory = CLSID("{7AB36653-1796-484B-BDFA-E74F1DB7C1DC}")

	// Interface IDs
	val IID_ISpellCheckerFactory = IID("{8E018A9D-2415-4677-BF08-794EA61F94BB}")
	val IID_ISpellChecker = IID("{B6FD0B71-E2BC-4653-8D05-F197E412770B}")
	val IID_ISpellChecker2 = IID("{E7ED1C71-87F7-4378-A840-C9200DACEE47}")
	val IID_IEnumSpellingError = IID("{803E3BD4-2828-4410-8290-418D1D73C762}")
	val IID_ISpellingError = IID("{B7C82D61-FBE8-4B47-9B27-6C0D2E0DE0A3}")
	val IID_IEnumString = IID("{00000101-0000-0000-C000-000000000046}")
	val IID_IOptionDescription = IID("{432E5F85-35CF-4606-A801-6F70277E1D7A}")
	val IID_IUserDictionariesRegistrar = IID("{AA176B85-0E12-4844-8E1A-EEF1DA77F586}")
}

/**
 * CORRECTIVE_ACTION enumeration values from spellcheck.h
 */
internal object CorrectiveAction {
	const val NONE = 0           // No errors
	const val GET_SUGGESTIONS = 1 // User should be prompted with suggestions
	const val REPLACE = 2         // Auto-replace with the suggestion
	const val DELETE = 3          // User should be prompted to delete
}
