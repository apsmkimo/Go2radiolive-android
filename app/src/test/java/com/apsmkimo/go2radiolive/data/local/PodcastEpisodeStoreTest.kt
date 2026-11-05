package com.apsmkimo.go2radiolive.data.local

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PodcastEpisodeStoreTest {
    @Test
    fun roundTripsEpisodesForOneShow() {
        val dir = File(System.getProperty("java.io.tmpdir"), "podcast-episodes-${System.nanoTime()}")
        val store = PodcastEpisodeStore(dir, Gson())
        val episodes = listOf(
            RadioStation(
                stationUuid = "podcast:ep-1",
                name = "第一集",
                streamUrl = "https://cdn.example/1.mp3",
                country = "Show",
                directStream = true,
                summary = "筆記",
                publishedAtMs = 10,
            ),
        )
        store.save("42", episodes)
        val reloaded = PodcastEpisodeStore(dir, Gson())
        assertEquals(episodes, reloaded.read("42"))
        assertTrue(reloaded.read("missing").isEmpty())
        store.save("42", emptyList())
        assertEquals(episodes, reloaded.read("42"))
        dir.deleteRecursively()
    }
}
