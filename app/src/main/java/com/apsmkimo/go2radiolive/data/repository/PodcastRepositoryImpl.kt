package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.local.PodcastCatalogStore
import com.apsmkimo.go2radiolive.data.remote.AppleStorefronts
import com.apsmkimo.go2radiolive.data.remote.PodcastRssParser
import com.apsmkimo.go2radiolive.data.remote.dedupePodcasts
import com.apsmkimo.go2radiolive.data.remote.podcastStorefront
import com.apsmkimo.go2radiolive.data.remote.retryIo
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.repository.PodcastRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Public Apple charts, no API key.
 *
 * The Marketing Tools chart
 * `https://rss.marketingtools.apple.com/api/v2/{country}/podcasts/top/100/podcasts.json`
 * returns at most 100 shows (a limit of 200 responds with HTTP 500). Extra shows
 * come from iTunes genre RSS feeds, also capped at 100, then deduped to 200.
 * The top chart and genre feeds run together, and the chart is emitted before
 * genres are merged. Episode audio comes from the iTunes Lookup `feedUrl` and
 * that show's RSS. A missing or unknown storefront code uses `us`. A known
 * region with an empty chart is replaced by the full US chart. A failed request
 * is reported as an error when nothing could be loaded.
 */
class PodcastRepositoryImpl(
    private val client: OkHttpClient,
    private val gson: Gson,
    private val catalogStore: PodcastCatalogStore,
) : PodcastRepository {

    override fun topShows(region: String?, bypassCache: Boolean): Flow<PodcastChart> = flow {
        val country = podcastStorefront(region)
        if (country == "us" || !AppleStorefronts.supports(country)) {
            catalogFlow("us", bypassCache, usedUsFallback = country != "us").collect { emit(it) }
            return@flow
        }
        var emitted = false
        try {
            catalogFlow(country, bypassCache, usedUsFallback = false).collect { chart ->
                if (chart.shows.isEmpty()) return@collect
                emitted = true
                emit(chart)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!emitted) throw error
            return@flow
        }
        if (!emitted) {
            catalogFlow("us", bypassCache, usedUsFallback = true).collect { emit(it) }
        }
    }

    override suspend fun episodes(
        show: PodcastShow,
        region: String?,
        bypassCache: Boolean,
    ): List<RadioStation> = withContext(Dispatchers.IO) {
        val country = podcastStorefront(region)
        if (!bypassCache) {
            val cachedFeed = catalogStore.feedUrl(show.id)
            if (!cachedFeed.isNullOrBlank()) {
                val cachedEpisodes = runCatching { readRss(cachedFeed, show, bypassCache = false) }.getOrNull()
                if (!cachedEpisodes.isNullOrEmpty()) return@withContext cachedEpisodes
            }
        }
        val feedUrl = lookupFeedUrl(show.id, country, bypassCache)
            ?: throw IOException("missing feed")
        readRss(feedUrl, show, bypassCache)
    }

    // SMCPKG_SUPPORT>>>Cursor103
    // private fun loadCatalog(country: String, bypassCache: Boolean): List<PodcastShow> {
    //     val groups = ArrayList<List<PodcastShow>>(1 + GENRES.size)
    //     groups.add(loadChart(country, bypassCache))
    //     for (genre in GENRES) {
    //         if (dedupePodcasts(groups, TARGET).size >= TARGET) break
    //         val extra = runCatching { loadGenre(country, genre, bypassCache) }.getOrDefault(emptyList())
    //         groups.add(extra)
    //     }
    //     return dedupePodcasts(groups, TARGET)
    // }
    // SMCPKG_SUPPORT<<<Cursor103

    private fun catalogFlow(
        country: String,
        bypassCache: Boolean,
        usedUsFallback: Boolean,
    ): Flow<PodcastChart> = flow {
        var last = emptyList<PodcastShow>()
        loadCatalogProgressive(country, bypassCache).collect { shows ->
            last = shows
            if (shows.isNotEmpty()) {
                emit(PodcastChart(shows, usedUsFallback = usedUsFallback, complete = false))
            }
        }
        emit(PodcastChart(last, usedUsFallback = usedUsFallback, complete = true))
    }

    private fun loadCatalogProgressive(country: String, bypassCache: Boolean): Flow<List<PodcastShow>> = flow {
        val downstream = this
        coroutineScope {
            val gate = Semaphore(PARALLEL_FEEDS)
            val chartDeferred = async {
                guarded { gate.withPermit { loadChart(country, bypassCache) } }
            }
            val genreDeferred = GENRES.map { genre ->
                async {
                    guarded { gate.withPermit { loadGenre(country, genre, bypassCache) } }
                }
            }
            val chartResult = chartDeferred.await()
            val groups = ArrayList<List<PodcastShow>>(1 + GENRES.size)
            groups.add(chartResult.getOrDefault(emptyList()))
            var emittedIds = emptyList<String>()
            suspend fun publish() {
                val merged = dedupePodcasts(groups, TARGET)
                if (merged.isEmpty()) return
                val ids = merged.map { it.id }
                if (ids == emittedIds) return
                emittedIds = ids
                downstream.emit(merged)
            }
            if (groups[0].isNotEmpty()) publish()
            var anyGenre = false
            for ((index, deferred) in genreDeferred.withIndex()) {
                if (dedupePodcasts(groups, TARGET).size >= TARGET) {
                    genreDeferred.drop(index).forEach { it.cancel() }
                    break
                }
                val extra = deferred.await().getOrDefault(emptyList())
                if (extra.isNotEmpty()) anyGenre = true
                groups.add(extra)
                publish()
            }
            val nothing = groups[0].isEmpty() && !anyGenre
            if (nothing && chartResult.isFailure) {
                throw chartResult.exceptionOrNull() ?: IOException("podcast catalog failed")
            }
            if (nothing && chartResult.isSuccess) downstream.emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadChart(country: String, bypassCache: Boolean): List<PodcastShow> {
        val url = "$CHART_ROOT/$country/podcasts/top/100/podcasts.json"
        val body = getString(url, bypassCache)
        val parsed = gson.fromJson(body, ChartResponse::class.java)
        return parsed.feed?.results.orEmpty().mapNotNull { it.toShow() }
    }

    private suspend fun loadGenre(country: String, genre: Int, bypassCache: Boolean): List<PodcastShow> {
        val url = "https://itunes.apple.com/$country/rss/toppodcasts/limit=100/genre=$genre/json"
        val body = getString(url, bypassCache)
        val feed = JsonParser.parseString(body).asJsonObject.getAsJsonObject("feed") ?: return emptyList()
        val entry = feed.get("entry") ?: return emptyList()
        val elements = when {
            entry.isJsonArray -> entry.asJsonArray
            entry.isJsonObject -> com.google.gson.JsonArray().apply { add(entry) }
            else -> return emptyList()
        }
        val shows = ArrayList<PodcastShow>(elements.size())
        for (element in elements) {
            val item = element.asJsonObject
            val id = item.getAsJsonObject("id")
                ?.getAsJsonObject("attributes")
                ?.get("im:id")
                ?.asString
                .orEmpty()
            val name = item.getAsJsonObject("im:name")?.get("label")?.asString.orEmpty()
            val author = item.getAsJsonObject("im:artist")?.get("label")?.asString.orEmpty()
            val images = item.getAsJsonArray("im:image")
            val artwork = if (images != null && images.size() > 0) {
                images[images.size() - 1].asJsonObject.get("label")?.asString.orEmpty()
            } else {
                ""
            }
            if (id.isNotBlank() && name.isNotBlank()) {
                shows.add(PodcastShow(id = id, name = name, author = author, artworkUrl = artwork))
            }
        }
        return shows
    }

    private suspend fun lookupFeedUrl(id: String, country: String, bypassCache: Boolean): String? {
        val body = getString("https://itunes.apple.com/lookup?id=$id&country=$country", bypassCache)
        val parsed = gson.fromJson(body, LookupResponse::class.java)
        val url = parsed.results.orEmpty().firstNotNullOfOrNull { result ->
            result.feedUrl?.trim()?.takeIf { it.isNotEmpty() }
        }
        if (!url.isNullOrBlank()) catalogStore.saveFeedUrl(id, url)
        return url
    }

    private suspend fun readRss(feedUrl: String, show: PodcastShow, bypassCache: Boolean): List<RadioStation> {
        return retryIo {
            val request = Request.Builder().url(feedUrl).build()
            client.newCall(bust(request, bypassCache)).execute().use { response ->
                if (!response.isSuccessful) throw IOException("rss ${response.code}")
                val body = response.body ?: throw IOException("empty rss")
                body.byteStream().use { stream ->
                    PodcastRssParser.parse(stream, show.name, show.artworkUrl)
                }
            }
        }
    }

    private fun bust(request: Request, bypassCache: Boolean): Request {
        if (!bypassCache) return request
        val url = request.url.newBuilder().addQueryParameter("cb", System.currentTimeMillis().toString()).build()
        return request.newBuilder()
            .url(url)
            .header("Cache-Control", "no-cache")
            .header("Pragma", "no-cache")
            .build()
    }

    private suspend fun getString(url: String, bypassCache: Boolean): String = retryIo {
        val request = bust(
            Request.Builder().url(url).header("Accept", "application/json").build(),
            bypassCache,
        )
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("http ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    private fun ChartItem.toShow(): PodcastShow? {
        val showId = id?.trim().orEmpty()
        val showName = name?.trim().orEmpty()
        if (showId.isEmpty() || showName.isEmpty()) return null
        return PodcastShow(
            id = showId,
            name = showName,
            author = artistName.orEmpty(),
            artworkUrl = artworkUrl100.orEmpty(),
        )
    }

    private data class ChartResponse(val feed: ChartFeed? = null)
    private data class ChartFeed(val results: List<ChartItem>? = null)
    private data class ChartItem(
        val id: String? = null,
        val name: String? = null,
        val artistName: String? = null,
        val artworkUrl100: String? = null,
    )
    private data class LookupResponse(val results: List<LookupItem>? = null)
    private data class LookupItem(
        @SerializedName("feedUrl") val feedUrl: String? = null,
    )

    companion object {
        private const val CHART_ROOT = "https://rss.marketingtools.apple.com/api/v2"
        private const val TARGET = 200
        private const val PARALLEL_FEEDS = 4

        /** Extra charts after the top-100 cap. Podcast genre ids from Apple. */
        private val GENRES = intArrayOf(
            1489,
            1324,
            1303,
            1304,
            1321,
            1488,
            1318,
            1309,
        )
    }
}

private suspend fun <T> guarded(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Result.failure(error)
    }
}
