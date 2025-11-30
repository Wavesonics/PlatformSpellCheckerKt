package com.darkrockstudios.libs.platformspellchecker

/**
 * Locale identifier for spell checking dictionaries.
 */
data class SpLocale(
	val language: String,
	val country: String? = null
) {
	companion object {
		// English
		val EN = SpLocale("en")
		val EN_US = SpLocale("en", "US")
		val EN_GB = SpLocale("en", "GB")
		val EN_IE = SpLocale("en", "IE")
		val EN_CA = SpLocale("en", "CA")
		val EN_AU = SpLocale("en", "AU")

		// Romance languages
		val ES = SpLocale("es")
		val ES_ES = SpLocale("es", "ES")
		val PT = SpLocale("pt")
		val PT_PT = SpLocale("pt", "PT")
		val PT_BR = SpLocale("pt", "BR")
		val FR = SpLocale("fr")
		val FR_FR = SpLocale("fr", "FR")
		val IT = SpLocale("it")
		val IT_IT = SpLocale("it", "IT")
		val RO = SpLocale("ro")
		val RO_RO = SpLocale("ro", "RO")

		// Germanic
		val DE = SpLocale("de")
		val DE_DE = SpLocale("de", "DE")
		val NL = SpLocale("nl")
		val NL_NL = SpLocale("nl", "NL")
		val SV = SpLocale("sv")
		val SV_SE = SpLocale("sv", "SE")
		val DA = SpLocale("da")
		val DA_DK = SpLocale("da", "DK")
		val NB = SpLocale("nb") // Norwegian Bokmål language code
		val NB_NO = SpLocale("nb", "NO") // Norwegian Bokmål
		val IS = SpLocale("is")
		val IS_IS = SpLocale("is", "IS")

		// Slavic and Baltic
		val PL = SpLocale("pl")
		val PL_PL = SpLocale("pl", "PL")
		val CS = SpLocale("cs")
		val CS_CZ = SpLocale("cs", "CZ")
		val SK = SpLocale("sk")
		val SK_SK = SpLocale("sk", "SK")
		val SL = SpLocale("sl")
		val SL_SI = SpLocale("sl", "SI")
		val HR = SpLocale("hr")
		val HR_HR = SpLocale("hr", "HR")
		val SR = SpLocale("sr")
		val SR_RS = SpLocale("sr", "RS")
		val BG = SpLocale("bg")
		val BG_BG = SpLocale("bg", "BG")
		val MK = SpLocale("mk")
		val MK_MK = SpLocale("mk", "MK")
		val UK = SpLocale("uk") // Ukrainian language code (may be confused with United Kingdom)
		val UK_UA = SpLocale("uk", "UA") // Ukrainian
		val LT = SpLocale("lt")
		val LT_LT = SpLocale("lt", "LT")
		val LV = SpLocale("lv")
		val LV_LV = SpLocale("lv", "LV")
		val ET = SpLocale("et")
		val ET_EE = SpLocale("et", "EE")

		// Uralic / Finno‑Ugric
		val FI = SpLocale("fi")
		val FI_FI = SpLocale("fi", "FI")
		val HU = SpLocale("hu")
		val HU_HU = SpLocale("hu", "HU")

		// Hellenic
		val EL = SpLocale("el")
		val EL_GR = SpLocale("el", "GR")

		// Others commonly encountered
		val TR_TR = SpLocale("tr", "TR") // Turkey
		val ZH_CN = SpLocale("zh", "CN") // Simplified Chinese
	}
}
