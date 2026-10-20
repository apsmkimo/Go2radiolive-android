package com.apsmkimo.go2radiolive.data.local

import com.apsmkimo.go2radiolive.data.remote.isPodcastCacheStale
import com.apsmkimo.go2radiolive.data.remote.retryIo
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.IOException

class PodcastCatalogStoreTest {
    @Test
    fun regionsAndFeedUrlsRoundTrip() {
        val dir = File(System.getProperty("java.io.tmpdir"), "podcast-cache-${System.nanoTime()}")
        dir.mkdirs()
        val store = PodcastCatalogStore(File(dir, "podcast_catalog.json"), Gson())
        val tw = PodcastChart(
            shows = listOf(PodcastShow(id = "1", name = "One", author = "A")),
            usedUsFallback = false,
        )
        val us = PodcastChart(
            shows = listOf(PodcastShow(id = "9", name = "Nine", author = "B", artworkUrl = "art")),
            usedUsFallback = true,
        )
        store.save("TW", tw, nowMs = 10)
        store.save("us", us, nowMs = 20)
        store.saveFeedUrl("1", "https://example.test/feed")

        val reloaded = PodcastCatalogStore(File(dir, "podcast_catalog.json"), Gson())
        assertEquals("1", reloaded.read("tw")?.shows?.single()?.id)
        assertEquals(false, reloaded.read("tw")?.usedUsFallback)
        assertEquals(10L, reloaded.read("tw")?.savedAtMs)
        assertEquals("art", reloaded.read("US")?.shows?.single()?.artworkUrl)
        assertEquals(true, reloaded.read("us")?.usedUsFallback)
        assertEquals("https://example.test/feed", reloaded.feedUrl("1"))
        assertNull(reloaded.read("jp"))
        dir.deleteRecursively()
    }

    @Test
    fun cacheIsStaleAtTwelveHours() {
        val saved = 1_000L
        val twelveHours = 12L * 60L * 60L * 1000L
        assertEquals(false, isPodcastCacheStale(saved, saved + twelveHours - 1))
        assertEquals(true, isPodcastCacheStale(saved, saved + twelveHours))
    }

    @Test
    fun retryStopsAfterTheFirstSuccess() = runBlocking {
        var calls = 0
        val value = retryIo(attempts = 3, initialDelayMs = 1) {
            calls += 1
            if (calls < 3) throw IOException("temporary")
            "ok"
        }
        assertEquals("ok", value)
        assertEquals(3, calls)
    }

    @Test
    fun retryGivesUpAfterTheLastAttempt() {
        assertThrows(IOException::class.java) {
            runBlocking {
                var calls = 0
                retryIo(attempts = 2, initialDelayMs = 1) {
                    calls += 1
                    throw IOException("down")
                }
            }
        }
    }
}
