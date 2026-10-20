package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.data.mapper.localizedCountryName
import com.apsmkimo.go2radiolive.domain.repository.COUNTRY_STATION_LIMIT
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun onlyAnEmptyRegionalChartUsesTheUsChart() {
        assertEquals(false, usesUsPodcastFallback("tw", chartFailed = true, catalogEmpty = false))
        assertEquals(false, usesUsPodcastFallback("tw", chartFailed = true, catalogEmpty = true))
        assertEquals(true, usesUsPodcastFallback("jp", chartFailed = false, catalogEmpty = true))
        assertEquals(false, usesUsPodcastFallback("tw", chartFailed = false, catalogEmpty = false))
        assertEquals(false, usesUsPodcastFallback("us", chartFailed = true, catalogEmpty = true))
    }

    @Test
    fun appleStorefrontListIncludesTaiwanAndRejectsUkAlias() {
        assertEquals(true, AppleStorefronts.supports("TW"))
        assertEquals(true, AppleStorefronts.supports("us"))
        assertEquals(false, AppleStorefronts.supports("uk"))
        assertEquals(true, AppleStorefronts.supports("gb"))
        assertEquals(AppleStorefronts.codes.size, AppleStorefronts.codes.distinct().size)
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
    fun anEmptyGenreFeedDoesNotDropTheChart() {
        val chart = listOf(PodcastShow(id = "a", name = "Chart", author = "A"))
        val merged = dedupePodcasts(listOf(chart, emptyList(), listOf(PodcastShow(id = "b", name = "Genre", author = "B"))))
        assertEquals(listOf("a", "b"), merged.map { it.id })
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
    fun countryStationLimitCoversMeasuredTaiwanIcrtRanks() {
        // 2026-10-19, hidebroken=true, clickcount desc: 193 healthy TW stations.
        // Radio ICRT rank 102, ICRT rank 159. Both lastcheckok=1.
        assertTrue(COUNTRY_STATION_LIMIT >= 193)
    }

    @Test
    fun countryNameUsesTheRequestedLocale() {
        val name = localizedCountryName("JP", "Japan", Locale.TRADITIONAL_CHINESE)
        assertEquals("日本", name)
    }
}
