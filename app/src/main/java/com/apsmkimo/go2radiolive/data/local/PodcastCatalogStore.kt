package com.apsmkimo.go2radiolive.data.local

import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.google.gson.Gson
import java.io.File

/**
 * Last successful podcast chart per storefront, plus iTunes Lookup feed URLs.
 * The file is the instant list shown when the Podcast tab opens.
 */
class PodcastCatalogStore(
    private val file: File,
    private val gson: Gson,
) {
    data class RegionEntry(
        val shows: List<PodcastShow>? = emptyList(),
        val usedUsFallback: Boolean = false,
        val savedAtMs: Long = 0,
    )

    private data class Snapshot(
        val regions: Map<String, RegionEntry>? = emptyMap(),
        val feedUrls: Map<String, String>? = emptyMap(),
    )

    private val lock = Any()
    private var memory: Snapshot? = null

    fun read(region: String): RegionEntry? = synchronized(lock) {
        val entry = load().regions.orEmpty()[normalize(region)] ?: return null
        val shows = entry.shows.orEmpty().filter { it.id.isNotBlank() && it.name.isNotBlank() }
        if (shows.isEmpty()) return null
        entry.copy(shows = shows)
    }

    fun save(region: String, chart: PodcastChart, nowMs: Long = System.currentTimeMillis()) = synchronized(lock) {
        if (chart.shows.isEmpty()) return
        val snap = load()
        val regions = LinkedHashMap(snap.regions.orEmpty())
        regions[normalize(region)] = RegionEntry(
            shows = chart.shows,
            usedUsFallback = chart.usedUsFallback,
            savedAtMs = nowMs,
        )
        persist(snap.copy(regions = regions))
    }

    fun feedUrl(showId: String): String? = synchronized(lock) {
        load().feedUrls.orEmpty()[showId]?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun saveFeedUrl(showId: String, url: String) = synchronized(lock) {
        val trimmed = url.trim()
        if (showId.isBlank() || trimmed.isEmpty()) return
        val snap = load()
        val feeds = LinkedHashMap(snap.feedUrls.orEmpty())
        if (feeds[showId] == trimmed) return
        feeds[showId] = trimmed
        persist(snap.copy(feedUrls = feeds))
    }

    private fun load(): Snapshot {
        memory?.let { return it }
        val parsed = if (file.isFile) {
            runCatching { gson.fromJson(file.readText(), Snapshot::class.java) }.getOrNull()
        } else {
            null
        }
        val snap = parsed ?: Snapshot()
        memory = snap
        return snap
    }

    private fun persist(snapshot: Snapshot) {
        memory = snapshot
        val parent = file.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${file.name}.tmp")
        tmp.writeText(gson.toJson(snapshot))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun normalize(region: String): String = region.trim().lowercase()
}
