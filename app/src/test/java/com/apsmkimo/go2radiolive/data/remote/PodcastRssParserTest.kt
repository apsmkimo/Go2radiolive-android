package com.apsmkimo.go2radiolive.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class PodcastRssParserTest {
    @Test
    fun readsPastTheOldHundredItemCapAndEmitsTheFirstScreenEarly() {
        val quick = ArrayList<Int>()
        val episodes = PodcastRssParser.parse(
            stream = rss(180).byteInputStream(),
            showName = "Show",
            artworkUrl = "https://cdn.example/art.jpg",
            quickCount = PODCAST_QUICK_EPISODE_COUNT,
            onQuick = { batch -> quick.add(batch.size) },
            parser = KXmlParser(),
        )
        assertEquals(listOf(PODCAST_QUICK_EPISODE_COUNT), quick)
        assertEquals(180, episodes.size)
        assertEquals("podcast:guid-1", episodes.first().stationUuid)
        assertEquals("podcast:guid-180", episodes.last().stationUuid)
        assertEquals("https://cdn.example/1.mp3", episodes.first().streamUrl)
        assertEquals("Show", episodes.first().country)
        assertTrue(episodes.first().directStream)
        assertEquals("Notes 1", episodes.first().summary)
        assertTrue(episodes.first().publishedAtMs > 0L)
        assertTrue(episodes.first().publishedAtMs >= episodes.last().publishedAtMs)
    }

    @Test
    fun malformedTailKeepsTheEpisodesAlreadyRead() {
        val xml = """
            <?xml version="1.0"?>
            <rss><channel>
            <item>
              <title>One</title>
              <guid>keep</guid>
              <enclosure url="https://cdn.example/1.mp3" type="audio/mpeg"/>
            </item>
            <item><title>Broken
            </channel></rss>
        """.trimIndent()
        val episodes = PodcastRssParser.parse(
            stream = xml.byteInputStream(),
            showName = "Show",
            artworkUrl = "",
            parser = KXmlParser(),
        )
        assertEquals(listOf("podcast:keep"), episodes.map { it.stationUuid })
    }

    private fun rss(count: Int): String {
        val items = buildString {
            for (index in 1..count) {
                append(
                    """
                    <item>
                      <title>Episode $index</title>
                      <guid>guid-$index</guid>
                      <pubDate>Tue, 07 Jan 2020 00:00:00 +0000</pubDate>
                      <enclosure url="https://cdn.example/$index.mp3" type="audio/mpeg"/>
                      <description>Notes $index</description>
                    </item>
                    """.trimIndent(),
                )
            }
        }
        return "<?xml version=\"1.0\"?><rss><channel>$items</channel></rss>"
    }

    private fun String.byteInputStream() = ByteArrayInputStream(toByteArray(StandardCharsets.UTF_8))
}
