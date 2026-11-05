package com.apsmkimo.go2radiolive.presentation.ui.components

// SMCPKG_SUPPORT>>>Cursor123
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeTest {
    @Test
    fun shortEpisodesUseMinutes() {
        assertEquals("00:00", formatPlaybackTime(0L, 600_000L))
        assertEquals("01:05", formatPlaybackTime(65_000L, 600_000L))
    }

    @Test
    fun hourLongEpisodesUseHours() {
        assertEquals("0:01:05", formatPlaybackTime(65_000L, 3_600_000L))
        assertEquals("1:02:03", formatPlaybackTime(3_723_000L, 3_723_000L))
    }
}
// SMCPKG_SUPPORT<<<Cursor123
