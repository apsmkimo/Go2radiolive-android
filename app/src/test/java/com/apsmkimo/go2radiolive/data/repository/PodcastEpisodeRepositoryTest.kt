package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.local.PodcastCatalogStore
import com.apsmkimo.go2radiolive.data.local.PodcastEpisodeStore
import com.apsmkimo.go2radiolive.data.remote.PodcastRssParser
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.kxml2.io.KXmlParser
import java.io.File
import java.util.concurrent.TimeUnit

class PodcastEpisodeRepositoryTest {
    private val server = MockWebServer()
    private val directory = File(System.getProperty("java.io.tmpdir"), "podcast-repo-${System.nanoTime()}")
    private val parserFactory = PodcastRssParser.newPullParser
    private val gson = Gson()

    @Before
    fun setUp() {
        PodcastRssParser.newPullParser = { KXmlParser() }
        server.start()
    }

    @After
    fun tearDown() {
        PodcastRssParser.newPullParser = parserFactory
        server.shutdown()
        directory.deleteRecursively()
    }

    @Test
    fun firstScreenArrivesBeforeTheRestOfTheFeedAndTheNextVisitUsesTheCache() = runBlocking {
        val rss = rss(160)
        server.enqueue(MockResponse().setBody(rss))
        val repository = repository()
        val show = show()
        val pages = repository.episodes(show, "tw", bypassCache = false).toList()
        assertTrue(pages.size >= 2)
        assertEquals(100, pages.first().episodes.size)
        assertTrue(pages.first().loadingMore)
        assertEquals("podcast:guid-1", pages.first().episodes.first().stationUuid)
        assertEquals(160, pages.last().episodes.size)
        assertFalse(pages.last().loadingMore)
        assertEquals("podcast:guid-160", pages.last().episodes.last().stationUuid)

        server.enqueue(MockResponse().setBody(rss).setBodyDelay(5, TimeUnit.SECONDS))
        val first = CompletableDeferred<Int>()
        val job = launch(Dispatchers.IO) {
            repository.episodes(show, "tw", bypassCache = false).collect { page ->
                if (!first.isCompleted) first.complete(page.episodes.size)
            }
        }
        assertEquals(160, withTimeout(1_000) { first.await() })
        job.cancel()
    }

    @Test
    fun aFailedRefreshKeepsTheCachedEpisodes() = runBlocking {
        server.enqueue(MockResponse().setBody(rss(120)))
        val repository = repository()
        val show = show()
        val loaded = repository.episodes(show, "us", bypassCache = false).toList()
        assertEquals(120, loaded.last().episodes.size)
        repeat(3) { server.enqueue(MockResponse().setResponseCode(500)) }
        val refreshed = repository.episodes(show, "us", bypassCache = true).toList()
        assertEquals(120, refreshed.last().episodes.size)
        assertFalse(refreshed.last().loadingMore)
    }

    @Test
    fun missingFeedUrlCompletesWithoutThrowing() = runBlocking {
        val repository = repository()
        val show = PodcastShow(id = "99", name = "No feed", author = "A")
        val pages = repository.episodes(show, "us", bypassCache = false).toList()
        assertEquals(1, pages.size)
        assertTrue(pages.single().episodes.isEmpty())
        assertFalse(pages.single().loadingMore)
    }

    private fun repository(): PodcastRepositoryImpl {
        return PodcastRepositoryImpl(
            client = OkHttpClient.Builder().addInterceptor { chain ->
                val url = chain.request().url.toString()
                if (url.contains("itunes.apple.com")) {
                    Response.Builder()
                        .request(chain.request())
                        .protocol(Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("{\"resultCount\":0,\"results\":[]}".toResponseBody("application/json".toMediaType()))
                        .build()
                } else {
                    chain.proceed(chain.request())
                }
            }.build(),
            gson = gson,
            catalogStore = PodcastCatalogStore(File(directory, "catalog.json"), gson),
            episodeStore = PodcastEpisodeStore(File(directory, "episodes"), gson),
        )
    }

    private fun show(): PodcastShow {
        return PodcastShow(
            id = "42",
            name = "Show",
            author = "Host",
            artworkUrl = "https://cdn.example/art.jpg",
            feedUrl = server.url("/feed.xml").toString(),
        )
    }

    private fun rss(count: Int): String {
        val items = buildString {
            for (index in 1..count) {
                append(
                    "<item><title>Episode $index</title><guid>guid-$index</guid>" +
                        "<enclosure url=\"https://cdn.example/$index.mp3\" type=\"audio/mpeg\"/>" +
                        "<description>Notes $index</description></item>",
                )
            }
        }
        return "<?xml version=\"1.0\"?><rss><channel>$items</channel></rss>"
    }
}
