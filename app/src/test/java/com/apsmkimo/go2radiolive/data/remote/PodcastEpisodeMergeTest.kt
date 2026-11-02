package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PodcastEpisodeMergeTest {
    @Test
    fun mergeKeepsRssOrderAndDropsGuidOrUrlDuplicates() {
        val cached = listOf(
            episode("podcast:old-guid", "Cached", "https://cdn.example/old.mp3"),
            episode("podcast:https://cdn.example/shared.mp3", "Url only", "https://cdn.example/shared.mp3"),
        )
        val incoming = listOf(
            episode("podcast:new", "Newest", "https://cdn.example/new.mp3"),
            episode("podcast:real-guid", "Shared", "https://cdn.example/shared.mp3"),
            episode("podcast:real-guid", "Shared again", "https://cdn.example/other.mp3"),
            episode("podcast:old-guid", "Cached refreshed", "https://cdn.example/old.mp3"),
        )
        val merged = mergePodcastEpisodes(cached, incoming)
        assertEquals(
            listOf("podcast:new", "podcast:real-guid", "podcast:old-guid"),
            merged.map { it.stationUuid },
        )
        assertEquals("Shared", merged[1].name)
        assertEquals("Cached refreshed", merged[2].name)
    }

    @Test
    fun keepsEpisodesThatWereOnlyInTheCache() {
        val cached = listOf(episode("podcast:gone", "Gone from feed", "https://cdn.example/gone.mp3"))
        val incoming = listOf(episode("podcast:live", "Live", "https://cdn.example/live.mp3"))
        val merged = mergePodcastEpisodes(cached, incoming)
        assertEquals(listOf("podcast:live", "podcast:gone"), merged.map { it.stationUuid })
    }

    @Test
    fun newestFirstLeavesADescendingFeedAloneAndFlipsAnAscendingOne() {
        val newest = listOf(
            episode("podcast:3", "Three", "https://cdn.example/3.mp3", publishedAtMs = 300),
            episode("podcast:2", "Two", "https://cdn.example/2.mp3", publishedAtMs = 200),
            episode("podcast:1", "One", "https://cdn.example/1.mp3", publishedAtMs = 100),
        )
        assertSame(newest, newestFirst(newest))
        val oldest = newest.asReversed()
        assertEquals(listOf("podcast:3", "podcast:2", "podcast:1"), newestFirst(oldest).map { it.stationUuid })
        val undated = newest.map { it.copy(publishedAtMs = 0) }
        assertSame(undated, newestFirst(undated))
    }

    @Test
    fun oldestDisplayReversesWithoutCopyingNewestOrder() {
        val episodes = listOf(
            episode("podcast:3", "Three", "https://cdn.example/3.mp3"),
            episode("podcast:1", "One", "https://cdn.example/1.mp3"),
        )
        assertSame(episodes, displayedPodcastEpisodes(episodes, newestFirst = true))
        assertEquals(
            listOf("podcast:1", "podcast:3"),
            displayedPodcastEpisodes(episodes, newestFirst = false).map { it.stationUuid },
        )
    }

    @Test
    fun parsesCommonRssDates() {
        assertTrue(parsePodcastDate("Tue, 01 Nov 2022 00:00:00 +0000") > 0L)
        assertTrue(parsePodcastDate("Tue, 01 Nov 2022 00:00:00 GMT") > 0L)
        assertEquals(0L, parsePodcastDate("not a date"))
        assertEquals(0L, parsePodcastDate("  "))
    }

    private fun episode(
        id: String,
        name: String,
        url: String,
        publishedAtMs: Long = 0,
    ) = RadioStation(
        stationUuid = id,
        name = name,
        streamUrl = url,
        directStream = true,
        publishedAtMs = publishedAtMs,
    )
}
