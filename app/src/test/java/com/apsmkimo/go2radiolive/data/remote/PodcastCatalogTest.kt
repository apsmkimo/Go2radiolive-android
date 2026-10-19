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
    fun failedOrEmptyRegionalChartUsesTheUsChart() {
        assertEquals(true, usesUsPodcastFallback("tw", chartFailed = true, catalogEmpty = false))
        assertEquals(true, usesUsPodcastFallback("jp", chartFailed = false, catalogEmpty = true))
        assertEquals(false, usesUsPodcastFallback("tw", chartFailed = false, catalogEmpty = false))
        assertEquals(false, usesUsPodcastFallback("us", chartFailed = true, catalogEmpty = true))
    }

    @Test
    fun localCountryPrefersNetworkThenSimThenLocale() {
        assertEquals("JP", preferredRegion("jp", "tw", "US"))
        assertEquals("TW", preferredRegion("", "tw", "US"))
        assertEquals("DE", preferredRegion(null, "??", "de"))
        assertEquals(null, preferredRegion("", "1", "usa"))
    }

    @Test
    fun localStationsKeepTaiwanAndFallBackToUs() {
        assertEquals("TW", localStationsCountry("tw", 0))
        assertEquals("US", localStationsCountry(null, 0))
        assertEquals("US", localStationsCountry("usa", 10))
        assertEquals("JP", localStationsCountry("jp", 3))
        assertEquals("US", localStationsCountry("jp", 0))
        assertEquals("US", localStationsCountry("us", 0))
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
