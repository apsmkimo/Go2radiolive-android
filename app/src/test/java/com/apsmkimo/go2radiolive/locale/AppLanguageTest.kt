package com.apsmkimo.go2radiolive.locale

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {
    @Test
    fun mapsKnownLocalesAndLeavesOthersOnSystem() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.selection("en", "US", ""))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.selection("ja", "JP", ""))
        assertEquals(AppLanguage.TRADITIONAL, AppLanguage.selection("zh", "TW", ""))
        assertEquals(AppLanguage.TRADITIONAL, AppLanguage.selection("zh", "HK", "Hant"))
        assertEquals(AppLanguage.SIMPLIFIED, AppLanguage.selection("zh", "CN", "Hans"))
        // SMCPKG_SUPPORT>>>Cursor126
        // assertEquals(AppLanguage.SYSTEM, AppLanguage.selection("fr", "FR", ""))
        assertEquals(AppLanguage.FRENCH, AppLanguage.selection("fr", "FR", ""))
        assertEquals(AppLanguage.PORTUGUESE_BR, AppLanguage.selection("pt", "BR", ""))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.selection("in", "ID", ""))
        assertEquals(AppLanguage.INDONESIAN, AppLanguage.selection("id", "ID", ""))
        // assertEquals(AppLanguage.SYSTEM, AppLanguage.selection("ar", "EG", ""))
        // SMCPKG_SUPPORT<<<Cursor126
        // SMCPKG_SUPPORT>>>Cursor127
        assertEquals(AppLanguage.ARABIC, AppLanguage.selection("ar", "EG", ""))
        assertEquals(AppLanguage.FILIPINO, AppLanguage.selection("tl", "PH", ""))
        assertEquals(AppLanguage.FILIPINO, AppLanguage.selection("fil", "PH", ""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.selection("he", "IL", ""))
        // SMCPKG_SUPPORT<<<Cursor127
    }

    // SMCPKG_SUPPORT>>>Cursor126
    @Test
    fun everyChoiceMapsBackToItself() {
        AppLanguage.tags.filter { it.isNotEmpty() }.forEach { tag ->
            val locale = java.util.Locale.forLanguageTag(tag)
            assertEquals(tag, AppLanguage.selection(locale.language, locale.country, locale.script))
        }
    }
    // SMCPKG_SUPPORT<<<Cursor126
}
