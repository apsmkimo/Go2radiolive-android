package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationDedupeTest {
    @Test
    fun mergesNamesThatDifferByCaseSpacingBracketsAndCommonSuffixes() {
        val merged = StationLists.dedupe(
            listOf(
                station("a", "Kiss 101.7 FM", "https://cdn.example/kiss-a", clicks = 10),
                station("b", "  kiss   fm  ", "https://cdn.example/kiss-b", clicks = 4),
                station("c", "Kiss (HD2)", "https://cdn.example/kiss-c", clicks = 3),
                station("d", "Kiss Live", "https://cdn.example/kiss-d", clicks = 2),
                station("e", "Radio ICRT", "https://cdn.example/icrt-a", clicks = 8),
                station("f", "ICRT [Live]", "https://cdn.example/icrt-b", clicks = 20),
            ),
        )
        assertEquals(listOf("a", "f"), merged.map { it.stationUuid })
        assertTrue(merged.first().alternateUrls.orEmpty().contains("https://cdn.example/kiss-b"))
        assertTrue(merged.first().alternateUrls.orEmpty().contains("https://cdn.example/kiss-d"))
        assertEquals("f", merged[1].stationUuid)
        assertEquals(listOf("e", "f"), merged[1].mergedUuids)
    }

    @Test
    fun doesNotMergeGenuinelyDifferentStations() {
        val merged = StationLists.dedupe(
            listOf(
                station("r1", "BBC Radio 1", "https://bbc.example/r1"),
                station("r2", "BBC Radio 2", "https://bbc.example/r2"),
                station("xtra", "BBC Radio 1Xtra", "https://bbc.example/1x"),
                station("tw", "Kiss FM", "https://tw.example/kiss", country = "TW"),
                station("us", "Kiss FM", "https://us.example/kiss", country = "US"),
                station("news", "News FM", "https://news.example/a"),
                station("news2", "News Live", "https://news.example/b"),
                station("capital", "Capital", "https://cap.example/main"),
                station("xtra2", "Capital Xtra", "https://cap.example/xtra"),
            ),
        )
        assertEquals(
            listOf("r1", "r2", "xtra", "tw", "us", "news", "news2", "capital", "xtra2"),
            merged.map { it.stationUuid },
        )
    }

    @Test
    fun mergesSameStreamHostAndPathAndIgnoresGenericPaths() {
        val merged = StationLists.dedupe(
            listOf(
                station("a", "Alpha One", "http://www.stream.example/icrt.mp3?sid=1"),
                station("b", "Completely Different", "https://stream.example/icrt.mp3"),
                station("c", "Shared A", "https://cdn.example/stream"),
                station("d", "Shared B", "https://cdn.example/stream?x=2"),
            ),
        )
        assertEquals(listOf("a", "c", "d"), merged.map { it.stationUuid })
        assertEquals(listOf("https://stream.example/icrt.mp3"), merged.first().alternateUrls)
    }

    @Test
    fun prefersLastCheckOkThenFaviconThenClicksVotesAndBitrate() {
        val checked = StationLists.dedupe(
            listOf(
                station("low", "Heart FM", "https://heart.example/a", clicks = 10, lastCheckOk = 1),
                station(
                    "busy",
                    "Heart",
                    "https://heart.example/b",
                    clicks = 9_000,
                    favicon = "https://heart.example/icon.png",
                    lastCheckOk = null,
                ),
            ),
        )
        assertEquals("low", checked.single().stationUuid)
        assertTrue(checked.single().alternateUrls.orEmpty().contains("https://heart.example/b"))

        val icon = StationLists.dedupe(
            listOf(
                station("plain", "Nova FM", "https://nova.example/a", clicks = 400, favicon = ""),
                station("art", "Nova", "https://nova.example/b", clicks = 20, favicon = "https://nova.example/f.png"),
            ),
        )
        assertEquals("art", icon.single().stationUuid)

        val clicks = StationLists.dedupe(
            listOf(
                station("small", "Orbit FM", "https://orbit.example/a", clicks = 5, favicon = "https://orbit.example/a.png"),
                station("big", "Orbit", "https://orbit.example/b", clicks = 80, favicon = "https://orbit.example/b.png"),
            ),
        )
        assertEquals("big", clicks.single().stationUuid)

        val votes = StationLists.dedupe(
            listOf(
                station("few", "Lumen FM", "https://lumen.example/a", clicks = 40, votes = 1, favicon = "https://lumen.example/a.png"),
                station("many", "Lumen", "https://lumen.example/b", clicks = 40, votes = 12, favicon = "https://lumen.example/b.png"),
            ),
        )
        assertEquals("many", votes.single().stationUuid)

        val bitrate = StationLists.dedupe(
            listOf(
                station("lowbr", "Wave FM", "https://wave.example/a", clicks = 10, votes = 3, bitrate = 64, favicon = "https://wave.example/a.png"),
                station("highbr", "Wave", "https://wave.example/b", clicks = 10, votes = 3, bitrate = 192, favicon = "https://wave.example/b.png"),
            ),
        )
        assertEquals("highbr", bitrate.single().stationUuid)
    }

    @Test
    fun filtersBrokenSslAndUnplayableCodecsButKeepsPodcastEpisodes() {
        val merged = StationLists.dedupe(
            listOf(
                station("ok", "Playable FM", "https://ok.example/a"),
                station("down", "Playable", "https://ok.example/broken", lastCheckOk = 0),
                station("ssl", "Secure FM", "https://ssl.example/a", sslError = true),
                station("wma", "Legacy FM", "https://wma.example/a", codec = "WMA"),
                station("hls", "Video FM", "https://hls.example/a", hls = true),
                station("ep1", "Episode One", "https://pod.example/1.mp3", direct = true, country = ""),
                station("ep2", "Episode One", "https://pod.example/2.mp3", direct = true, country = ""),
            ),
        )
        assertEquals(listOf("ok", "ep1", "ep2"), merged.map { it.stationUuid })
        assertTrue(merged.first().alternateUrls.isNullOrEmpty())
    }

    @Test
    fun localRankUsesVotesAndARecentSuccessfulCheck() {
        val now = 1_700_000_000_000L
        val fresh = station("fresh", "Fresh FM", "https://fresh.example/a", clicks = 100, lastCheckTimeMs = now - 86_400_000L)
        val stale = station("stale", "Stale FM", "https://stale.example/a", clicks = 100, lastCheckTimeMs = now - 40L * 86_400_000L)
        val voted = station("voted", "Voted FM", "https://voted.example/a", clicks = 100, votes = 30, lastCheckTimeMs = now - 40L * 86_400_000L)
        val popular = station("popular", "Popular FM", "https://popular.example/a", clicks = 200, lastCheckTimeMs = now - 40L * 86_400_000L)
        val ranked = StationLists.present(listOf(stale, voted, fresh, popular), emptyMap(), now, rankLocal = true)
        assertEquals(listOf("popular", "voted", "fresh", "stale"), ranked.map { it.stationUuid })
        assertTrue(StationLists.localRankScore(fresh, now) > StationLists.localRankScore(stale, now))
        assertTrue(StationLists.localRankScore(voted, now) > StationLists.localRankScore(stale, now))
        assertTrue(StationLists.localRankScore(popular, now) > StationLists.localRankScore(fresh, now))
        assertFalse(StationLists.recentlyVerified(stale, now))
        assertNull(StationLists.nameKey("FM"))
    }

    private fun station(
        id: String,
        name: String,
        url: String,
        country: String = "TW",
        favicon: String = "",
        clicks: Int = 0,
        votes: Int = 0,
        bitrate: Int = 0,
        lastCheckOk: Int? = 1,
        lastCheckTimeMs: Long = 0,
        codec: String? = "MP3",
        sslError: Boolean = false,
        hls: Boolean = false,
        direct: Boolean = false,
    ) = RadioStation(
        stationUuid = id,
        name = name,
        streamUrl = url,
        faviconUrl = favicon,
        countryCode = country,
        clickCount = clicks,
        votes = votes,
        bitrate = bitrate,
        lastCheckOk = lastCheckOk,
        lastCheckTimeMs = lastCheckTimeMs,
        codec = codec,
        sslError = sslError,
        hls = hls,
        directStream = direct,
    )
}
