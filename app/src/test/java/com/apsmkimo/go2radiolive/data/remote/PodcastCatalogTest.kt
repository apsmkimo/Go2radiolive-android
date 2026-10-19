package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.data.mapper.localizedCountryName
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PodcastCatalogTest {
    @Test
    fun storefrontFallsBackToUs() {
        assertEquals("us", podcastStorefront(null))
        assertEquals("us", podcastStorefront(""))
        assertEquals("us", podcastStorefront("usa"))
        assertEquals("tw", podcastStorefront("TW"))
    }

    @Test
    fun chartsAreDedupedToTwoHundred() {
        val first = (1..100).map { PodcastShow(id = it.toString(), name = "Show $it", author = "A") }
        val overlap = (80..220).map { PodcastShow(id = it.toString(), name = "Extra $it", author = "B") }
        val merged = dedupePodcasts(listOf(first, overlap), limit = 200)
        assertEquals(200, merged.size)
        assertEquals("1", merged.first().id)
        assertEquals(200, merged.map { it.id }.distinct().size)
    }

    @Test
    fun countryNameUsesTheRequestedLocale() {
        val name = localizedCountryName("JP", "Japan", Locale.TRADITIONAL_CHINESE)
        assertEquals("日本", name)
    }
}
