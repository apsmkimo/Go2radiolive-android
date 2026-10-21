package com.apsmkimo.go2radiolive.locale

/**
 * Per-app language tags. Empty means follow the phone language.
 * Names of the fixed choices are shown in that language, not via string resources.
 */
internal object AppLanguage {
    const val SYSTEM = ""
    const val ENGLISH = "en"
    const val TRADITIONAL = "zh-TW"
    const val SIMPLIFIED = "zh-CN"
    const val JAPANESE = "ja"

    val tags: List<String> = listOf(SYSTEM, ENGLISH, TRADITIONAL, SIMPLIFIED, JAPANESE)

    fun selection(language: String, country: String, script: String): String {
        val lang = language.lowercase()
        val region = country.uppercase()
        val writing = script.lowercase()
        return when {
            lang == "en" -> ENGLISH
            lang == "ja" -> JAPANESE
            lang == "zh" && (region == "CN" || writing == "hans") -> SIMPLIFIED
            lang == "zh" && (region == "TW" || region == "HK" || region == "MO" || writing == "hant") ->
                TRADITIONAL
            lang == "zh" -> SIMPLIFIED
            else -> SYSTEM
        }
    }
}
