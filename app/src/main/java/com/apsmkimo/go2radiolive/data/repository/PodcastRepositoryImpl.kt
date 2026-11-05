package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.local.PodcastCatalogStore
import com.apsmkimo.go2radiolive.data.local.PodcastEpisodeStore
import com.apsmkimo.go2radiolive.data.remote.AppleStorefronts
import com.apsmkimo.go2radiolive.data.remote.PODCAST_MAX_EPISODES
import com.apsmkimo.go2radiolive.data.remote.PODCAST_QUICK_EPISODE_COUNT
import com.apsmkimo.go2radiolive.data.remote.PodcastRssParser
import com.apsmkimo.go2radiolive.data.remote.dedupePodcasts
import com.apsmkimo.go2radiolive.data.remote.itunesPodcastSearchUrl
import com.apsmkimo.go2radiolive.data.remote.mapItunesPodcastSearch
import com.apsmkimo.go2radiolive.data.remote.mergePodcastEpisodes
import com.apsmkimo.go2radiolive.data.remote.newestFirst
import com.apsmkimo.go2radiolive.data.remote.podcastStorefront
import com.apsmkimo.go2radiolive.data.remote.retryIo
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastEpisodePage
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.repository.PodcastRepository
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
    // SMCPKG_SUPPORT>>>Cursor122
    private val episodeStore: PodcastEpisodeStore,
    // SMCPKG_SUPPORT<<<Cursor122
) : PodcastRepository {

    // SMCPKG_SUPPORT>>>Cursor122
    /** The chart client times out too soon for a multi-megabyte feed. The first items still emit as they arrive. */
    private val rssClient: OkHttpClient = client.newBuilder()
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()
    // SMCPKG_SUPPORT<<<Cursor122

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

    // SMCPKG_SUPPORT>>>Cursor122
    // override suspend fun episodes(
    //     show: PodcastShow,
    //     region: String?,
    //     bypassCache: Boolean,
    // ): List<RadioStation> = withContext(Dispatchers.IO) {
    //     val country = podcastStorefront(region)
    //     val embedded = show.feedUrl?.trim()?.takeIf { it.isNotEmpty() }
    //     if (embedded != null) {
    //         catalogStore.saveFeedUrl(show.id, embedded)
    //         try {
    //             return@withContext readRss(embedded, show, bypassCache)
    //         } catch (cancelled: CancellationException) {
    //             throw cancelled
    //         } catch (_: Exception) {
    //             // The search feed failed. Fall through to the cached or lookup feed.
    //         }
    //     }
    //     if (!bypassCache) {
    //         val cachedFeed = catalogStore.feedUrl(show.id)
    //         if (!cachedFeed.isNullOrBlank() && cachedFeed != embedded) {
    //             val cachedEpisodes = runCatching { readRss(cachedFeed, show, bypassCache = false) }.getOrNull()
    //             if (!cachedEpisodes.isNullOrEmpty()) return@withContext cachedEpisodes
    //         }
    //     }
    //     val feedUrl = lookupFeedUrl(show.id, country, bypassCache)
    //         ?: throw IOException("missing feed")
    //     readRss(feedUrl, show, bypassCache)
    // }
    override fun episodes(
        show: PodcastShow,
        region: String?,
        bypassCache: Boolean,
    ): Flow<PodcastEpisodePage> = channelFlow {
        val cached = mergePodcastEpisodes(emptyList(), episodeStore.read(show.id)).takeIf { it.isNotEmpty() }
        if (cached != null && !bypassCache) send(PodcastEpisodePage(cached, loadingMore = true))
        var quickList = emptyList<RadioStation>()
        try {
            val parsed = loadFeed(show, region, bypassCache) { quick ->
                val ordered = newestFirst(mergePodcastEpisodes(cached.orEmpty(), quick))
                quickList = ordered
                // A partial feed must not replace a longer saved list.
                if (cached == null && ordered.isNotEmpty()) {
                    episodeStore.save(show.id, ordered)
                    trySend(PodcastEpisodePage(ordered, loadingMore = true))
                }
            }
            ensureActive()
            if (parsed == null) {
                if (cached != null) {
                    send(PodcastEpisodePage(cached, loadingMore = false))
                } else {
                    send(PodcastEpisodePage(emptyList(), loadingMore = false))
                }
                return@channelFlow
            }
            val merged = newestFirst(mergePodcastEpisodes(cached.orEmpty(), parsed))
            val page = when {
                merged.isNotEmpty() -> merged
                cached != null -> cached
                else -> quickList
            }
            if (page.isNotEmpty()) episodeStore.save(show.id, page)
            send(PodcastEpisodePage(page, loadingMore = false))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            if (!isActive) throw CancellationException("episodes cancelled", error)
            val fallback = cached ?: quickList.takeIf { it.isNotEmpty() }
            if (fallback != null) {
                send(PodcastEpisodePage(fallback, loadingMore = false))
            } else {
                throw error
            }
        }
    }.flowOn(Dispatchers.IO)
    // SMCPKG_SUPPORT<<<Cursor122

    // SMCPKG_SUPPORT>>>Cursor115
    override suspend fun searchShows(
        term: String,
        region: String?,
        bypassCache: Boolean,
    ): List<PodcastShow> = withContext(Dispatchers.IO) {
        val query = term.trim()
        if (query.isEmpty()) return@withContext emptyList()
        val country = podcastStorefront(region)
        val primary = fetchPodcastSearch(query, country, bypassCache)
        if (primary.isNotEmpty() || country == "us") return@withContext primary
        val withoutCountry = runCatching { fetchPodcastSearch(query, null, bypassCache) }
            .getOrElse { error ->
                if (error is CancellationException) throw error
                emptyList()
            }
        if (withoutCountry.isNotEmpty()) return@withContext withoutCountry
        fetchPodcastSearch(query, "us", bypassCache)
    }

    private suspend fun fetchPodcastSearch(
        term: String,
        country: String?,
        bypassCache: Boolean,
    ): List<PodcastShow> {
        val body = getStringCancellable(itunesPodcastSearchUrl(term, country), bypassCache)
        return mapItunesPodcastSearch(body)
    }
    // SMCPKG_SUPPORT<<<Cursor115

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

    // SMCPKG_SUPPORT>>>Cursor122
    // private suspend fun readRss(feedUrl: String, show: PodcastShow, bypassCache: Boolean): List<RadioStation> {
    //     return retryIo {
    //         val request = Request.Builder().url(feedUrl).build()
    //         client.newCall(bust(request, bypassCache)).execute().use { response ->
    //             if (!response.isSuccessful) throw IOException("rss ${response.code}")
    //             val body = response.body ?: throw IOException("empty rss")
    //             body.byteStream().use { stream ->
    //                 PodcastRssParser.parse(stream, show.name, show.artworkUrl)
    //             }
    //         }
    //     }
    // }
    /**
     * Tries the show's own feed, then a saved feed URL, then iTunes Lookup.
     * Returns null when no feed URL exists. A bad document falls through to the next URL.
     */
    private suspend fun loadFeed(
        show: PodcastShow,
        region: String?,
        bypassCache: Boolean,
        onQuick: (List<RadioStation>) -> Unit,
    ): List<RadioStation>? {
        val country = podcastStorefront(region)
        val embedded = show.feedUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (embedded != null) catalogStore.saveFeedUrl(show.id, embedded)
        val candidates = LinkedHashSet<String>()
        if (embedded != null) candidates.add(embedded)
        if (!bypassCache) {
            val saved = catalogStore.feedUrl(show.id)
            if (!saved.isNullOrBlank()) candidates.add(saved)
        }
        var lastError: Exception? = null
        for (url in candidates) {
            try {
                return readRss(url, show, bypassCache, onQuick)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                lastError = error
            }
        }
        var lookupFailed: Exception? = null
        val lookedUp = runCatching { lookupFeedUrl(show.id, country, bypassCache) }.getOrElse { error ->
            if (error is CancellationException) throw error
            lookupFailed = error as? Exception ?: IOException("lookup failed")
            null
        }
        if (!lookedUp.isNullOrBlank() && lookedUp !in candidates) {
            return readRss(lookedUp, show, bypassCache, onQuick)
        }
        // No feed URL at all is an empty list. A failed request still surfaces when nothing was read.
        if (candidates.isEmpty() && lookedUp.isNullOrBlank() && lookupFailed == null) return null
        throw lookupFailed ?: lastError ?: IOException("rss failed")
    }

    private suspend fun readRss(
        feedUrl: String,
        show: PodcastShow,
        bypassCache: Boolean,
        onQuick: (List<RadioStation>) -> Unit,
    ): List<RadioStation> {
        return retryIo {
            coroutineContext.ensureActive()
            val request = bust(
                Request.Builder()
                    .url(feedUrl)
                    .header("Accept", "application/rss+xml, application/xml, text/xml, */*")
                    .build(),
                bypassCache,
            )
            val call = rssClient.newCall(request)
            coroutineContext[Job]?.invokeOnCompletion { cause ->
                if (cause != null) call.cancel()
            }
            call.execute().use { response ->
                if (!response.isSuccessful) throw IOException("rss ${response.code}")
                val body = response.body ?: throw IOException("empty rss")
                body.byteStream().use { stream ->
                    PodcastRssParser.parse(
                        stream,
                        show.name,
                        show.artworkUrl,
                        limit = PODCAST_MAX_EPISODES,
                        quickCount = PODCAST_QUICK_EPISODE_COUNT,
                        onQuick = onQuick,
                    )
                }
            }
        }
    }
    // SMCPKG_SUPPORT<<<Cursor122

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

    // SMCPKG_SUPPORT>>>Cursor115
    /** Same cache and retry as [getString], but the call is cancelled when the coroutine is. */
    private suspend fun getStringCancellable(url: String, bypassCache: Boolean): String = retryIo {
        coroutineContext.ensureActive()
        val request = bust(
            Request.Builder().url(url).header("Accept", "application/json").build(),
            bypassCache,
        )
        suspendCancellableCoroutine { cont ->
            val call = client.newCall(request)
            cont.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!cont.isActive) return
                    if (call.isCanceled()) {
                        cont.resumeWithException(CancellationException("podcast search cancelled", e))
                    } else {
                        cont.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.use { payload ->
                            if (!payload.isSuccessful) throw IOException("http ${payload.code}")
                            payload.body?.string().orEmpty()
                        }
                        if (cont.isActive) cont.resume(body)
                    } catch (error: Exception) {
                        if (!cont.isActive) return
                        if (call.isCanceled() || error is CancellationException) {
                            cont.resumeWithException(CancellationException("podcast search cancelled", error))
                        } else {
                            cont.resumeWithException(error)
                        }
                    }
                }
            })
        }
    }
    // SMCPKG_SUPPORT<<<Cursor115

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
