package com.apsmkimo.go2radiolive.domain.station

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationPlaceholderTest {
    @Test
    fun homepageFaviconUsesOnlyTheStationsOwnOrigin() {
        assertEquals(
            "https://example.com/favicon.ico",
            StationImages.homepageFavicon("https://Example.com/station/home"),
        )
        assertEquals(
            "http://example.com:8080/favicon.ico",
            StationImages.homepageFavicon("http://example.com:8080/radio/"),
        )
        assertEquals(
            "https://example.com/favicon.ico",
            StationImages.homepageFavicon("https://example.com:443/path"),
        )
        assertNull(StationImages.homepageFavicon(""))
        assertNull(StationImages.homepageFavicon("not a url"))
        assertNull(StationImages.homepageFavicon("javascript:alert(1)"))
        assertNull(StationImages.homepageFavicon("https://user:pass@example.com/home"))
        val own = StationImages.homepageFavicon("https://radio.example/home")
        assertEquals("https://radio.example/favicon.ico", own)
        assertFalse(own!!.contains("google"))
        assertFalse(own.contains("duckduckgo"))
    }

    @Test
    fun placeholderUsesTheFirstCharacterAndAStableColor() {
        assertEquals("I", StationImages.placeholderLetter("icrt"))
        assertEquals("好", StationImages.placeholderLetter("  好事903"))
        assertEquals("1", StationImages.placeholderLetter("101.7 Kiss"))
        assertEquals("?", StationImages.placeholderLetter("   "))
        assertEquals("?", StationImages.placeholderLetter("!!!"))

        val first = StationImages.placeholderColorArgb("  ICRT ")
        assertEquals(first, StationImages.placeholderColorArgb("icrt"))
        assertEquals(0xFF, first ushr 24)
        assertTrue(first in StationImages.PALETTE.toList())
        val colors = listOf("Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf")
            .map { StationImages.placeholderColorArgb(it) }
        assertTrue(colors.toSet().size > 1)
        assertEquals(
            StationImages.placeholderColorArgb("好事903"),
            StationImages.placeholderColorArgb("好事903"),
        )
    }
}
