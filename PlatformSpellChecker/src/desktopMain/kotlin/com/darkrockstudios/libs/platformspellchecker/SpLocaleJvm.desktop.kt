package com.darkrockstudios.libs.platformspellchecker

/** JVM-only convenience conversions between [SpLocale] and [java.util.Locale] (Desktop/JVM).
 */
fun SpLocale.toJavaLocale(): java.util.Locale {
    return if (country.isNullOrBlank()) {
        java.util.Locale(language)
    } else {
        java.util.Locale(language, country)
    }
}

fun java.util.Locale.toSpLocale(): SpLocale {
    val lang = language.ifBlank { "" }
    val ctry = country.ifBlank { null }
    return SpLocale(lang, ctry)
}
