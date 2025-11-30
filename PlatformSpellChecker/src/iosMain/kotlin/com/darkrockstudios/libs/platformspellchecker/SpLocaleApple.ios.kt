package com.darkrockstudios.libs.platformspellchecker

import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleCountryCode
import platform.Foundation.NSLocaleLanguageCode
import platform.Foundation.localeIdentifier

/** iOS-only convenience conversions between [SpLocale] and [NSLocale] (Apple platforms).
 */
fun SpLocale.toNSLocale(): NSLocale {
	// Apple locale identifiers commonly use underscore separators like "en_US"
	val identifier = if (country.isNullOrBlank()) language else $$"$language_$country"
	return NSLocale(localeIdentifier = identifier)
}

fun NSLocale.toSpLocale(): SpLocale {
	// Prefer structured values from NSLocale keys. Fallback to parsing identifier if needed.
	val lang = (this.objectForKey(NSLocaleLanguageCode) as? String)?.ifBlank { null }
	val ctry = (this.objectForKey(NSLocaleCountryCode) as? String)?.ifBlank { null }

	val language =
		(lang ?: (this.localeIdentifier.let { it.substringBefore('_').ifBlank { null } } ?: "")).ifBlank { "" }
	val country = ctry ?: this.localeIdentifier.substringAfter('_', missingDelimiterValue = "").ifBlank { null }
	return SpLocale(language, country)
}
