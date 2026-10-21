package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.mapper.toDomainStations
import com.apsmkimo.go2radiolive.data.remote.CatalogTopics
import com.apsmkimo.go2radiolive.data.remote.RadioBrowserApi
import com.apsmkimo.go2radiolive.data.remote.cacheBustToken
import com.apsmkimo.go2radiolive.data.remote.cacheControlHeader
import com.apsmkimo.go2radiolive.data.remote.retryIo
import com.apsmkimo.go2radiolive.domain.model.CatalogLanguage
import com.apsmkimo.go2radiolive.domain.model.CatalogPage
import com.apsmkimo.go2radiolive.domain.model.CatalogSort
import com.apsmkimo.go2radiolive.domain.model.CategoryHub
import com.apsmkimo.go2radiolive.domain.model.CountResult
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import android.util.Log
import java.io.IOException

/**
 * Radio-Browser rankings and tag searches for the category tab.
 * Tag synonyms run together with a small concurrency limit. One failed tag
 * does not fail the list. A failed ranking does not hide the others.
 */
class CategoryCatalogRepository(
    private val api: RadioBrowserApi,
) {
    fun hub(countryCode: String?, bypassCache: Boolean): Flow<CategoryHub> = flow {
        coroutineScope {
            val votes = async { catchIo { ranked(countryCode, "votes", true, bypassCache) } }
            val clicks = async { catchIo { ranked(countryCode, "clickcount", true, bypassCache) } }
            val trend = async { catchIo { ranked(countryCode, "clicktrend", true, bypassCache) } }
            val counts = async { catchIo { topicCounts(countryCode, bypassCache) } }
            val languages = async { catchIo { languages(countryCode, bypassCache) } }
            val voteList = votes.await()
            val clickList = clicks.await()
            val trendList = trend.await()
            emit(
                CategoryHub(
                    votes = voteList.getOrNull(),
                    clicks = clickList.getOrNull(),
                    trend = trendList.getOrNull(),
                    complete = false,
                ),
            )
            val countMap = counts.await()
            val languageList = languages.await()
            val anySuccess = listOf(voteList, clickList, trendList, countMap, languageList).any { it.isSuccess }
            if (!anySuccess) {
                val cause = listOf(voteList, clickList, trendList, countMap, languageList)
                    .firstNotNullOfOrNull { it.exceptionOrNull() }
                if (cause is Exception && cause !is CancellationException) throw cause
                throw IOException("catalog")
            }
            emit(
                CategoryHub(
                    votes = voteList.getOrNull(),
                    clicks = clickList.getOrNull(),
                    trend = trendList.getOrNull(),
                    counts = countMap.getOrDefault(emptyMap()),
                    languages = languageList.getOrNull(),
                    complete = true,
                ),
            )
        }
    }.flowOn(Dispatchers.IO)

    fun page(
        countryCode: String?,
        kind: String,
        sort: CatalogSort,
        language: String,
        highBitrate: Boolean,
        bypassCache: Boolean,
    ): Flow<CatalogPage> = flow {
        val local = loadKind(countryCode, kind, sort, language, highBitrate, bypassCache)
        val page = CatalogPage(stations = local, others = emptyList(), complete = false)
        if (countryCode.isNullOrBlank() || !CatalogTopics.needsOtherCountries(local.size)) {
            emit(page.copy(complete = true))
            return@flow
        }
        emit(page)
        val others = catchIo {
            loadKind(null, kind, sort, language, highBitrate, bypassCache)
        }
        if (others.isFailure) {
            emit(CatalogPage(stations = local, others = emptyList(), complete = false))
            return@flow
        }
        val seen = local.map { it.stationUuid }.toHashSet()
        val extra = others.getOrDefault(emptyList())
            .filter { it.stationUuid !in seen }
            .take(CatalogTopics.TAG_LIMIT)
        emit(CatalogPage(stations = local, others = extra, complete = true))
    }.flowOn(Dispatchers.IO)

    private suspend fun loadKind(
        countryCode: String?,
        kind: String,
        sort: CatalogSort,
        language: String,
        highBitrate: Boolean,
        bypassCache: Boolean,
    ): List<RadioStation> {
        val bitrate = if (highBitrate) CatalogTopics.HIGH_BITRATE else null
        return when {
            kind == CatalogTopics.RANK_VOTES -> ranked(countryCode, "votes", true, bypassCache, language, bitrate)
            kind == CatalogTopics.RANK_CLICKS -> ranked(countryCode, "clickcount", true, bypassCache, language, bitrate)
            kind == CatalogTopics.RANK_TREND -> ranked(countryCode, "clicktrend", true, bypassCache, language, bitrate)
            kind.startsWith("lang:") -> {
                val name = kind.removePrefix("lang:")
                val (order, reverse) = orderOf(sort)
                searchOne(
                    countryCode = countryCode,
                    tag = null,
                    order = order,
                    reverse = reverse,
                    language = name,
                    bitrateMin = bitrate,
                    limit = CatalogTopics.LIST_LIMIT,
                    bypassCache = bypassCache,
                )
            }
            kind.startsWith("topic:") -> {
                val topic = kind.removePrefix("topic:")
                val tags = CatalogTopics.searchTags(topic)
                val (order, reverse) = orderOf(sort)
                val fetched = fetchTags(
                    tags = tags,
                    countryCode = countryCode,
                    order = order,
                    reverse = reverse,
                    language = language,
                    bitrateMin = bitrate,
                    limit = CatalogTopics.TAG_LIMIT,
                    bypassCache = bypassCache,
                )
                CatalogTopics.applyFilters(
                    CatalogTopics.mergeStations(fetched.groups, sort, CatalogTopics.LIST_LIMIT),
                    language,
                    highBitrate,
                )
            }
            else -> emptyList()
        }
    }

    private suspend fun ranked(
        countryCode: String?,
        order: String,
        reverse: Boolean,
        bypassCache: Boolean,
        language: String = "",
        bitrateMin: Int? = null,
    ): List<RadioStation> {
        return searchOne(
            countryCode = countryCode,
            tag = null,
            order = order,
            reverse = reverse,
            language = language,
            bitrateMin = bitrateMin,
            limit = CatalogTopics.RANK_LIMIT,
            bypassCache = bypassCache,
        )
    }

    private suspend fun topicCounts(countryCode: String?, bypassCache: Boolean): Map<String, CountResult> {
        val topics = CatalogTopics.topicIds()
        if (countryCode.isNullOrBlank()) {
            val directory = tagDirectory(bypassCache)
            return topics.associateWith { CatalogTopics.countFromDirectory(it, directory) }
        }
        return coroutineScope {
            val gate = Semaphore(PARALLEL)
            var successes = 0
            var failures = 0
            val pairs = topics.map { topic ->
                async {
                    topic to catchIo {
                        val fetched = fetchTags(
                            tags = CatalogTopics.countTags(topic),
                            countryCode = countryCode,
                            order = "clickcount",
                            reverse = true,
                            language = "",
                            bitrateMin = null,
                            limit = CatalogTopics.TAG_LIMIT,
                            bypassCache = bypassCache,
                            gate = gate,
                        )
                        val ids = HashSet<String>()
                        for (group in fetched.groups) {
                            for (station in group) {
                                if (station.stationUuid.isNotBlank()) ids.add(station.stationUuid)
                            }
                        }
                        CountResult(count = ids.size, capped = fetched.capped)
                    }
                }
            }.awaitAll()
            val counts = LinkedHashMap<String, CountResult>()
            for ((topic, result) in pairs) {
                val value = result.getOrNull()
                if (value == null) {
                    failures += 1
                } else {
                    successes += 1
                    counts[topic] = value
                }
            }
            if (successes == 0 && failures > 0) throw IOException("counts failed")
            counts
        }
    }

    private suspend fun languages(countryCode: String?, bypassCache: Boolean): List<CatalogLanguage> {
        if (countryCode.isNullOrBlank()) {
            val rows = retryIo {
                api.getLanguages(
                    order = "stationcount",
                    reverse = true,
                    limit = 80,
                    cacheControl = cacheControlHeader(bypassCache),
                    cacheBust = cacheBustToken(bypassCache),
                )
            }
            return rows.mapNotNull { row ->
                val name = row.name?.trim().orEmpty()
                val count = row.stationCount ?: 0
                if (!CatalogTopics.usefulLanguage(name) || count <= 0) null
                else CatalogLanguage(name = name.lowercase(), stationCount = count)
            }.take(40)
        }
        val sample = searchOne(
            countryCode = countryCode,
            tag = null,
            order = "clickcount",
            reverse = true,
            language = "",
            bitrateMin = null,
            limit = CatalogTopics.LANGUAGE_SAMPLE,
            bypassCache = bypassCache,
        )
        return CatalogTopics.tallyLanguages(sample).take(40)
    }

    private suspend fun tagDirectory(bypassCache: Boolean): Map<String, Int> {
        val tags = retryIo {
            api.getTags(
                order = "stationcount",
                reverse = true,
                limit = 1000,
                cacheControl = cacheControlHeader(bypassCache),
                cacheBust = cacheBustToken(bypassCache),
            )
        }
        val directory = HashMap<String, Int>(tags.size)
        for (tag in tags) {
            val name = tag.name?.trim()?.lowercase().orEmpty()
            if (name.isEmpty()) continue
            directory[name] = tag.stationCount ?: 0
        }
        return directory
    }

    private suspend fun fetchTags(
        tags: List<String>,
        countryCode: String?,
        order: String,
        reverse: Boolean,
        language: String,
        bitrateMin: Int?,
        limit: Int,
        bypassCache: Boolean,
        gate: Semaphore = Semaphore(PARALLEL),
    ): TagFetch = coroutineScope {
        if (tags.isEmpty()) return@coroutineScope TagFetch(emptyList(), capped = false, failures = 0, successes = 0)
        val results = tags.map { tag ->
            async {
                gate.withPermit {
                    catchIo {
                        searchOne(countryCode, tag, order, reverse, language, bitrateMin, limit, bypassCache)
                    }
                }
            }
        }.awaitAll()
        val groups = ArrayList<List<RadioStation>>()
        var failures = 0
        var successes = 0
        var capped = false
        for (result in results) {
            val list = result.getOrNull()
            if (list == null) {
                failures += 1
            } else {
                successes += 1
                if (list.size >= limit) capped = true
                groups.add(list)
            }
        }
        if (successes == 0 && failures > 0) {
            throw IOException("tags failed")
        }
        TagFetch(groups, capped, failures, successes)
    }

    private suspend fun searchOne(
        countryCode: String?,
        tag: String?,
        order: String,
        reverse: Boolean,
        language: String,
        bitrateMin: Int?,
        limit: Int,
        bypassCache: Boolean,
    ): List<RadioStation> {
        val lang = language.trim().takeIf { it.isNotEmpty() }
        val rows = retryIo {
            api.searchStations(
                limit = limit,
                hideBroken = true,
                order = order,
                reverse = reverse,
                countryCode = countryCode?.takeIf { it.isNotBlank() },
                tag = tag,
                tagExact = if (tag.isNullOrBlank()) null else true,
                language = lang,
                languageExact = if (lang == null) null else true,
                bitrateMin = bitrateMin,
                cacheControl = cacheControlHeader(bypassCache),
                cacheBust = cacheBustToken(bypassCache),
            )
        }
        return rows.toDomainStations()
    }

    private fun orderOf(sort: CatalogSort): Pair<String, Boolean> = when (sort) {
        CatalogSort.CLICKS -> "clickcount" to true
        CatalogSort.VOTES -> "votes" to true
        CatalogSort.NAME -> "name" to false
    }

    private data class TagFetch(
        val groups: List<List<RadioStation>>,
        val capped: Boolean,
        val failures: Int,
        val successes: Int,
    )

    companion object {
        private const val PARALLEL = 4
    }
}

private suspend fun <T> catchIo(block: suspend () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Throwable) {
        Log.w(TAG, "catalog request failed", error)
        Result.failure(error)
    }
}

private const val TAG = "CategoryCatalog"
