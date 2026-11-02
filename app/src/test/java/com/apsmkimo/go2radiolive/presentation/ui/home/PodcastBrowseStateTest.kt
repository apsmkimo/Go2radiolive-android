package com.apsmkimo.go2radiolive.presentation.ui.home

import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SMCPKG_SUPPORT>>>Cursor117
class PodcastBrowseStateTest {
    @Test
    fun searchToolbarStaysOnRadioAndTheShowList() {
        assertTrue(showsBrowseSearchToolbar(BrowseTab.LOCAL, null))
        assertTrue(showsBrowseSearchToolbar(BrowseTab.WORLD, null))
        assertTrue(showsBrowseSearchToolbar(BrowseTab.CATEGORIES, null))
        assertTrue(showsBrowseSearchToolbar(BrowseTab.FAVORITES, null))
        assertTrue(showsBrowseSearchToolbar(BrowseTab.PODCAST, null))
    }

    @Test
    fun searchToolbarHidesOnAnOpenShow() {
        val show = PodcastShow(id = "1", name = "很長的節目名稱", author = "作者")
        assertFalse(showsBrowseSearchToolbar(BrowseTab.PODCAST, show))
        assertTrue(showsBrowseSearchToolbar(BrowseTab.LOCAL, show))
    }

    @Test
    fun parkedSearchDoesNotFilterEpisodesAndComesBackWithTheShowList() {
        val episode = RadioStation(
            stationUuid = "podcast:1",
            name = "單集標題很長也不該被搜尋字濾掉",
            streamUrl = "https://example.test/a.mp3",
            summary = "完整說明",
            directStream = true,
        )
        val results = listOf(PodcastShow(id = "jazz", name = "Jazz Hour", author = "A"))
        val open = HomeUiState(
            selectedTab = BrowseTab.PODCAST,
            selectedPodcast = PodcastShow(id = "1", name = "Show", author = "A"),
            searchQuery = "jazz",
            stations = listOf(episode),
            podcastSearchResults = results,
        )
        assertEquals(listOf(episode), open.visibleStations)
        val back = open.copy(selectedPodcast = null, stations = emptyList())
        assertEquals("jazz", back.searchQuery)
        assertEquals(results, back.visiblePodcasts)
        assertTrue(showsBrowseSearchToolbar(back.selectedTab, back.selectedPodcast))
    }

    @Test
    fun oldestSortPutsTheEarliestEpisodeFirstWithoutDroppingAny() {
        val episodes = (3 downTo 1).map { number ->
            RadioStation(
                stationUuid = "podcast:$number",
                name = "Episode $number",
                streamUrl = "https://cdn.example/$number.mp3",
                directStream = true,
            )
        }
        val open = HomeUiState(
            selectedTab = BrowseTab.PODCAST,
            selectedPodcast = PodcastShow(id = "1", name = "Show", author = "A"),
            stations = episodes,
            episodeSortNewest = false,
        )
        assertEquals(listOf("podcast:1", "podcast:2", "podcast:3"), open.visibleStations.map { it.stationUuid })
        assertEquals(episodes, open.copy(episodeSortNewest = true).visibleStations)
    }

    @Test
    fun backgroundPageDoesNotShrinkTheListOrRaiseAnError() {
        val full = List(120) { index ->
            RadioStation(
                stationUuid = "podcast:$index",
                name = "Episode $index",
                streamUrl = "https://cdn.example/$index.mp3",
                directStream = true,
            )
        }
        val state = HomeUiState(
            selectedTab = BrowseTab.PODCAST,
            selectedPodcast = PodcastShow(id = "9", name = "Show", author = "A"),
            stations = full,
        )
        val shorter = state.withEpisodePage(
            com.apsmkimo.go2radiolive.domain.model.PodcastEpisodePage(full.take(100), loadingMore = true),
            showId = "9",
        )
        assertEquals(full, shorter.stations)
        assertTrue(shorter.episodesLoadingMore)
        assertEquals(null, shorter.errorResId)
        val failed = state.withEpisodePage(
            com.apsmkimo.go2radiolive.domain.model.PodcastEpisodePage(emptyList(), loadingMore = false),
            showId = "9",
        )
        assertEquals(full, failed.stations)
        assertFalse(failed.episodesLoadingMore)
    }
}
// SMCPKG_SUPPORT<<<Cursor117