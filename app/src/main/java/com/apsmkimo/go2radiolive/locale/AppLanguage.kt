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
    // SMCPKG_SUPPORT>>>Cursor126
    const val SPANISH = "es"
    const val PORTUGUESE_BR = "pt-BR"
    const val FRENCH = "fr"
    const val GERMAN = "de"
    const val KOREAN = "ko"
    const val RUSSIAN = "ru"
    const val INDONESIAN = "id"
    const val TURKISH = "tr"
    const val ITALIAN = "it"
    const val VIETNAMESE = "vi"
    // SMCPKG_SUPPORT<<<Cursor126

    // SMCPKG_SUPPORT>>>Cursor126
    // val tags: List<String> = listOf(SYSTEM, ENGLISH, TRADITIONAL, SIMPLIFIED, JAPANESE)
    val tags: List<String> = listOf(
        SYSTEM,
        ENGLISH,
        TRADITIONAL,
        SIMPLIFIED,
        JAPANESE,
        SPANISH,
        PORTUGUESE_BR,
        FRENCH,
        GERMAN,
        KOREAN,
        RUSSIAN,
        INDONESIAN,
        TURKISH,
        ITALIAN,
        VIETNAMESE,
    )

    /** Each language's own name, so a user can find it whatever the current UI language is. */
    fun nativeName(tag: String): String = when (tag) {
        ENGLISH -> "English"
        TRADITIONAL -> "繁體中文"
        SIMPLIFIED -> "简体中文"
        JAPANESE -> "日本語"
        SPANISH -> "Español"
        PORTUGUESE_BR -> "Português (Brasil)"
        FRENCH -> "Français"
        GERMAN -> "Deutsch"
        KOREAN -> "한국어"
        RUSSIAN -> "Русский"
        INDONESIAN -> "Bahasa Indonesia"
        TURKISH -> "Türkçe"
        ITALIAN -> "Italiano"
        VIETNAMESE -> "Tiếng Việt"
        else -> tag
    }
    // SMCPKG_SUPPORT<<<Cursor126

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
            // SMCPKG_SUPPORT>>>Cursor126
            lang == "es" -> SPANISH
            lang == "pt" -> PORTUGUESE_BR
            lang == "fr" -> FRENCH
            lang == "de" -> GERMAN
            lang == "ko" -> KOREAN
            lang == "ru" -> RUSSIAN
            // Android reports Indonesian with the legacy code "in".
            lang == "id" || lang == "in" -> INDONESIAN
            lang == "tr" -> TURKISH
            lang == "it" -> ITALIAN
            lang == "vi" -> VIETNAMESE
            // SMCPKG_SUPPORT<<<Cursor126
            else -> SYSTEM
        }
    }
}
