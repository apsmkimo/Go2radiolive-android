package com.apsmkimo.go2radiolive.data.remote

import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PodcastSearchTest {
    @Test
    fun searchUrlEncodesTermAndStorefront() {
        assertEquals(
            "https://itunes.apple.com/search?media=podcast&entity=podcast&term=npr+news&limit=200&country=tw",
            itunesPodcastSearchUrl(" npr news ", "TW"),
        )
        assertEquals(
            "https://itunes.apple.com/search?media=podcast&entity=podcast&term=hello&limit=200",
            itunesPodcastSearchUrl("hello", null),
        )
        assertEquals(
            "https://itunes.apple.com/search?media=podcast&entity=podcast&term=hello&limit=200",
            itunesPodcastSearchUrl("hello", "  "),
        )
        assertEquals(
            "https://itunes.apple.com/search?media=podcast&entity=podcast&term=hello&limit=200&country=us",
            itunesPodcastSearchUrl("hello", "us"),
        )
    }

    @Test
    fun mapsArtworkTitleAuthorAndFeedAndSkipsRowsWithoutAFeed() {
        val json = """
            {
              "resultCount": 4,
              "results": [
                {
                  "collectionId": 42,
                  "collectionName": "City Lights",
                  "artistName": "Ada",
                  "feedUrl": "https://example.test/city.xml",
                  "artworkUrl60": "https://example.test/60.jpg",
                  "artworkUrl100": "https://example.test/100.jpg",
                  "artworkUrl600": "https://example.test/600.jpg"
                },
                {
                  "collectionId": 7,
                  "collectionName": "No Feed",
                  "artistName": "Bea",
                  "artworkUrl100": "https://example.test/x.jpg"
                },
                {
                  "collectionId": 42,
                  "collectionName": "Duplicate",
                  "artistName": "Ada",
                  "feedUrl": "https://example.test/other.xml"
                },
                {
                  "trackId": 99,
                  "trackName": "Track Title",
                  "artistName": "Cy",
                  "feedUrl": " https://example.test/track.xml ",
                  "artworkUrl100": "https://example.test/t100.jpg"
                }
              ]
            }
        """.trimIndent()

        val shows = mapItunesPodcastSearch(json)

        assertEquals(2, shows.size)
        assertEquals("42", shows[0].id)
        assertEquals("City Lights", shows[0].name)
        assertEquals("Ada", shows[0].author)
        assertEquals("https://example.test/600.jpg", shows[0].artworkUrl)
        assertEquals("https://example.test/city.xml", shows[0].feedUrl)
        assertEquals("99", shows[1].id)
        assertEquals("Track Title", shows[1].name)
        assertEquals("Cy", shows[1].author)
        assertEquals("https://example.test/t100.jpg", shows[1].artworkUrl)
        assertEquals("https://example.test/track.xml", shows[1].feedUrl)
    }

    @Test
    fun blankFeedBlankNameAndEmptyPayloadAreDropped() {
        val json = """
            {
              "results": [
                {"collectionId": 1, "collectionName": "Blank", "feedUrl": "  "},
                {"collectionId": 2, "feedUrl": "https://example.test/noname.xml", "artistName": "Z"},
                {"collectionId": "3", "collectionName": "Kept", "artistName": "Dee", "feedUrl": "https://example.test/kept.xml"}
              ]
            }
        """.trimIndent()

        val shows = mapItunesPodcastSearch(json)

        assertEquals(1, shows.size)
        assertEquals("3", shows[0].id)
        assertEquals("Kept", shows[0].name)
        assertEquals("Dee", shows[0].author)
        assertEquals("", shows[0].artworkUrl)
        assertEquals(emptyList<Any>(), mapItunesPodcastSearch("""{"resultCount":0,"results":[]}"""))
    }

    @Test
    fun searchMappingStopsAtTwoHundred() {
        val rows = (1..205).joinToString(",") { index ->
            """{"collectionId":$index,"collectionName":"Show $index","artistName":"A","feedUrl":"https://example.test/$index.xml","artworkUrl100":"https://example.test/$index.jpg"}"""
        }
        val shows = mapItunesPodcastSearch("""{"results":[$rows]}""")
        assertEquals(200, shows.size)
        assertEquals("1", shows.first().id)
        assertEquals("200", shows.last().id)
        assertEquals("https://example.test/200.xml", shows.last().feedUrl)
    }

    @Test
    fun malformedSearchJsonThrows() {
        assertThrows(JsonSyntaxException::class.java) {
            mapItunesPodcastSearch("not-json")
        }
        assertThrows(JsonSyntaxException::class.java) {
            mapItunesPodcastSearch("{")
        }
    }
}
