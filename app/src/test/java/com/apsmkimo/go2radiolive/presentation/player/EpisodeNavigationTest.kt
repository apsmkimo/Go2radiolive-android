package com.apsmkimo.go2radiolive.presentation.player

// SMCPKG_SUPPORT>>>Cursor123
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EpisodeNavigationTest {
    private fun ep(id: String) = RadioStation(stationUuid = id, name = id, streamUrl = "https://x/$id.mp3", directStream = true)

    // Newest first, as the feed lists them.
    private val list = listOf(ep("e3"), ep("e2"), ep("e1"))

    @Test
    fun nextIsNewerRowAbove() {
        assertEquals("e3", EpisodeNavigation.newer(list, "e2")?.stationUuid)
        assertNull(EpisodeNavigation.newer(list, "e3"))
    }

    @Test
    fun previousIsOlderRowBelow() {
        assertEquals("e1", EpisodeNavigation.older(list, "e2")?.stationUuid)
        assertNull(EpisodeNavigation.older(list, "e1"))
    }

    @Test
    fun endsDisableButtons() {
        val newest = EpisodeNavigation.nav(list, "e3")
        assertFalse(newest.hasNewer)
        assertTrue(newest.hasOlder)
        val oldest = EpisodeNavigation.nav(list, "e1")
        assertTrue(oldest.hasNewer)
        assertFalse(oldest.hasOlder)
    }

    @Test
    fun unknownEpisodeHasNoNavigation() {
        val nav = EpisodeNavigation.nav(list, "other")
        assertNull(nav.currentId)
        assertFalse(nav.hasNewer)
        assertFalse(nav.hasOlder)
        assertNull(EpisodeNavigation.newer(list, null))
    }
}
// SMCPKG_SUPPORT<<<Cursor123
