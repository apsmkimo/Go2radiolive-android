package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistParserTest {
    @Test
    fun parsesPlsAndM3uAndResolvesRelativeUrls() {
        val pls = """
            [playlist]
            NumberOfEntries=2
            File1=http://example.com/a.mp3
            Title1=One
            File2=https://example.com/b
            Length1=-1
        """.trimIndent()
        assertEquals(
            listOf("http://example.com/a.mp3", "https://example.com/b"),
            PlaylistParser.parse(pls, "http://example.com/listen.pls"),
        )

        val m3u = """
            #EXTM3U
            #EXTINF:-1,Name
            http://example.com/main.mp3
            # comment
            live.mp3
            /backup.mp3
        """.trimIndent()
        assertEquals(
            listOf(
                "http://example.com/main.mp3",
                "http://example.com/live.mp3",
                "http://example.com/backup.mp3",
            ),
            PlaylistParser.parse(m3u, "http://example.com/listen.m3u"),
        )
        assertTrue(PlaylistParser.isPlaylistUrl("https://example.com/a.pls?token=1"))
        assertFalse(PlaylistParser.isPlaylistUrl("https://example.com/a.mp3"))
    }

    @Test
    fun skipsHlsSegmentsAndNonHttpTargets() {
        val hls = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXTINF:6.0,
            seg0.ts
            #EXTINF:6.0,
            https://cdn.example/seg1.m4s
            file:///tmp/secret.mp3
            javascript:alert(1)
        """.trimIndent()
        assertTrue(PlaylistParser.parse(hls, "https://cdn.example/live.m3u8").isEmpty())

        val asx = """
            <asx version="3.0">
              <entry><ref href="http://example.com/live.mp3" /></entry>
              <entry><ref href="file:///tmp/x.mp3" /></entry>
            </asx>
        """.trimIndent()
        assertEquals(listOf("http://example.com/live.mp3"), PlaylistParser.parse(asx, "http://example.com/a.asx"))
    }

    @Test
    fun playbackCandidatesKeepClickUrlThenStreamThenAlternates() {
        val station = RadioStation(
            stationUuid = "s",
            name = "Example",
            streamUrl = "https://example.com/main.mp3",
            alternateUrls = listOf(
                "https://example.com/backup.mp3",
                "https://example.com/main.mp3",
                "not a url",
            ),
        )
        assertEquals(
            listOf(
                "https://example.com/resolved.m3u",
                "https://example.com/main.mp3",
                "https://example.com/backup.mp3",
            ),
            PlaylistParser.playbackCandidates(station, "https://example.com/resolved.m3u"),
        )
    }
}
