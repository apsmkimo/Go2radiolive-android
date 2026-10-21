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
        assertEquals(AppLanguage.SYSTEM, AppLanguage.selection("fr", "FR", ""))
    }
}
