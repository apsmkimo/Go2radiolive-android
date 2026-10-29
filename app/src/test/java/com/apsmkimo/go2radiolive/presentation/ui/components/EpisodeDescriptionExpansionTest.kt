package com.apsmkimo.go2radiolive.presentation.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

// SMCPKG_SUPPORT>>>Cursor118
class EpisodeDescriptionExpansionTest {
    @Test
    fun toggleIsPerEpisodeAndStartsCollapsed() {
        val state = EpisodeDescriptionExpansion()
        assertFalse(state.isExpanded("podcast:a"))
        assertFalse(state.isExpanded("podcast:b"))
        state.toggle("podcast:a")
        assertTrue(state.isExpanded("podcast:a"))
        assertFalse(state.isExpanded("podcast:b"))
        state.toggle("podcast:a")
        assertFalse(state.isExpanded("podcast:a"))
    }

    @Test
    fun overflowStaysWithTheEpisodeAcrossToggle() {
        val state = EpisodeDescriptionExpansion()
        assertFalse(state.overflows("podcast:a"))
        state.markOverflow("podcast:a")
        state.toggle("podcast:a")
        assertTrue(state.overflows("podcast:a"))
        assertTrue(state.isExpanded("podcast:a"))
        assertFalse(state.overflows("podcast:b"))
        state.markOverflow("podcast:a")
        assertTrue(state.overflows("podcast:a"))
    }

    @Test
    fun aFreshStateDoesNotKeepAnotherShowsExpansion() {
        val firstVisit = EpisodeDescriptionExpansion()
        firstVisit.toggle("podcast:ep-1")
        val nextVisit = EpisodeDescriptionExpansion()
        assertFalse(nextVisit.isExpanded("podcast:ep-1"))
    }

    @Test
    fun onlyOverflowingOrOpenDescriptionsShowTheControl() {
        assertFalse(showEpisodeDescriptionToggle(hasOverflow = false, expanded = false))
        assertTrue(showEpisodeDescriptionToggle(hasOverflow = true, expanded = false))
        assertTrue(showEpisodeDescriptionToggle(hasOverflow = true, expanded = true))
        assertEquals(3, episodeDescriptionMaxLines(expanded = false))
        assertEquals(Int.MAX_VALUE, episodeDescriptionMaxLines(expanded = true))
    }

    @Test
    fun localeFilesUseTheRequestedLabels() {
        assertEquals("See more", stringValue("values", "episode_see_more"))
        assertEquals("Show less", stringValue("values", "episode_show_less"))
        assertEquals("檢視更多", stringValue("values-zh-rTW", "episode_see_more"))
        assertEquals("顯示較少", stringValue("values-zh-rTW", "episode_show_less"))
        assertEquals("檢視更多", stringValue("values-zh-rHK", "episode_see_more"))
        assertEquals("顯示較少", stringValue("values-zh-rHK", "episode_show_less"))
        assertEquals("查看更多", stringValue("values-zh-rCN", "episode_see_more"))
        assertEquals("显示较少", stringValue("values-zh-rCN", "episode_show_less"))
        assertEquals("もっと見る", stringValue("values-ja", "episode_see_more"))
        assertEquals("閉じる", stringValue("values-ja", "episode_show_less"))
    }

    private fun stringValue(folder: String, name: String): String {
        val text = stringsFile(folder).readText()
        val match = Regex("""<string name="$name">([^<]*)</string>""").find(text)
        return match?.groupValues?.get(1) ?: error("missing $name in $folder")
    }

    private fun stringsFile(folder: String): File {
        val relative = "src/main/res/$folder/strings.xml"
        val candidates = listOf(
            File(relative),
            File("app/$relative"),
            File("app/src/main/res/$folder/strings.xml"),
        )
        return candidates.firstOrNull { it.isFile } ?: error("missing $relative from ${File(".").absolutePath}")
    }
}
// SMCPKG_SUPPORT<<<Cursor118
