package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.data.local.CategoryCatalogStore
import com.apsmkimo.go2radiolive.domain.model.CatalogSort
import com.apsmkimo.go2radiolive.domain.model.CategoryHub
import com.apsmkimo.go2radiolive.domain.model.CountResult
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTopicsTest {
    @Test
    fun synonymsCoverEnglishChineseAndJapanese() {
        val pop = CatalogTopics.searchTags(CatalogTopics.POP)
        assertTrue(pop.contains("pop"))
        assertTrue(pop.contains("流行"))
        assertTrue(pop.contains("流行音乐"))
        assertTrue(pop.contains("ポップ"))
        val oldies = CatalogTopics.searchTags(CatalogTopics.OLDIES)
        assertTrue(oldies.contains("oldies"))
        assertTrue(oldies.contains("懷舊"))
        assertTrue(oldies.contains("怀旧"))
        assertTrue(oldies.contains("懐メロ"))
        assertTrue(CatalogTopics.searchTags(CatalogTopics.D80).contains("80年代"))
        assertTrue(CatalogTopics.searchTags(CatalogTopics.NEWS).contains("ニュース"))
    }

    @Test
    fun mergeKeepsHigherClickCountAndDropsDuplicates() {
        val first = station("a", "Alpha", clicks = 5)
        val duplicate = station("a", "Alpha FM", clicks = 9)
        val second = station("b", "Beta", clicks = 7)
        val merged = CatalogTopics.mergeStations(listOf(listOf(first), listOf(duplicate, second)), CatalogSort.CLICKS)
        assertEquals(listOf("a", "b"), merged.map { it.stationUuid })
        assertEquals(9, merged.first().clickCount)
    }

    @Test
    fun fewerThanTenLocalStationsNeedOtherCountries() {
        assertTrue(CatalogTopics.needsOtherCountries(9))
        assertFalse(CatalogTopics.needsOtherCountries(10))
    }

    @Test
    fun directoryCountsOnlyExactSynonyms() {
        val directory = mapOf("pop" to 100, "synthpop" to 50, "流行" to 3)
        val count = CatalogTopics.countFromDirectory(CatalogTopics.POP, directory)
        assertEquals(103, count.count)
    }

    @Test
    fun parentCountSumsChildrenAndKeepsCap() {
        val counts = mapOf(
            CatalogTopics.NEWS to CountResult(4, capped = false),
            CatalogTopics.TALK to CountResult(6, capped = true),
        )
        val parent = CatalogTopics.parentCount(CatalogTopics.NEWS_TALK, counts)
        assertEquals(10, parent.count)
        assertTrue(parent.capped)
    }

    @Test
    fun bitrateAndLanguageFilters() {
        val stations = listOf(
            station("a", "A", bitrate = 64, language = "english"),
            station("b", "B", bitrate = 128, language = "chinese,mandarin"),
            station("c", "C", bitrate = 192, language = "japanese"),
        )
        val highChinese = CatalogTopics.applyFilters(stations, "chinese", highBitrate = true)
        assertEquals(listOf("b"), highChinese.map { it.stationUuid })
    }

    @Test
    fun hubCacheIsSeparatePerScope() {
        val dir = File(System.getProperty("java.io.tmpdir"), "catalog-cache-${System.nanoTime()}")
        dir.mkdirs()
        val store = CategoryCatalogStore(File(dir, "category_catalog.json"), Gson())
        store.saveHub(
            "TW",
            CategoryHub(clicks = listOf(station("a", "A")), counts = mapOf("pop" to CountResult(2))),
            nowMs = 5,
        )
        store.saveHub("GLOBAL", CategoryHub(votes = listOf(station("b", "B"))), nowMs = 6)
        val reloaded = CategoryCatalogStore(File(dir, "category_catalog.json"), Gson())
        assertEquals("a", reloaded.readHub("TW")?.clicks?.single()?.stationUuid)
        assertEquals(2, reloaded.readHub("TW")?.counts?.get("pop")?.count)
        assertEquals("b", reloaded.readHub("GLOBAL")?.votes?.single()?.stationUuid)
        dir.deleteRecursively()
    }

    private fun station(
        id: String,
        name: String,
        clicks: Int = 0,
        bitrate: Int = 0,
        language: String = "",
    ) = RadioStation(
        stationUuid = id,
        name = name,
        streamUrl = "https://example.test/$id",
        clickCount = clicks,
        bitrate = bitrate,
        language = language,
    )
}
