package com.apsmkimo.go2radiolive.data.local

import com.apsmkimo.go2radiolive.domain.model.CatalogLanguage
import com.apsmkimo.go2radiolive.domain.model.CatalogPage
import com.apsmkimo.go2radiolive.domain.model.CategoryHub
import com.apsmkimo.go2radiolive.domain.model.CountResult
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import java.io.File

/** Last successful category hub and station lists, keyed by scope and category. */
class CategoryCatalogStore(
    private val file: File,
    private val gson: Gson,
) {
    data class HubEntry(
        val votes: List<RadioStation>? = emptyList(),
        val clicks: List<RadioStation>? = emptyList(),
        val trend: List<RadioStation>? = emptyList(),
        val counts: Map<String, CountResult>? = emptyMap(),
        val languages: List<CatalogLanguage>? = emptyList(),
        val savedAtMs: Long = 0,
    )

    data class ListEntry(
        val stations: List<RadioStation>? = emptyList(),
        val others: List<RadioStation>? = emptyList(),
        val savedAtMs: Long = 0,
    )

    private data class Snapshot(
        val hubs: Map<String, HubEntry>? = emptyMap(),
        val lists: Map<String, ListEntry>? = emptyMap(),
    )

    private val lock = Any()
    private var memory: Snapshot? = null

    fun readHub(scope: String): HubEntry? = synchronized(lock) {
        val entry = load().hubs.orEmpty()[scope] ?: return null
        if (entry.savedAtMs <= 0) return null
        val useful = entry.votes.orEmpty().isNotEmpty() ||
            entry.clicks.orEmpty().isNotEmpty() ||
            entry.trend.orEmpty().isNotEmpty() ||
            entry.counts.orEmpty().isNotEmpty() ||
            entry.languages.orEmpty().isNotEmpty()
        if (!useful) null else entry
    }

    fun saveHub(scope: String, hub: CategoryHub, nowMs: Long = System.currentTimeMillis()) = synchronized(lock) {
        if (!hub.complete) return
        val snap = load()
        val hubs = LinkedHashMap(snap.hubs.orEmpty())
        hubs[scope] = HubEntry(
            votes = hub.votes,
            clicks = hub.clicks,
            trend = hub.trend,
            counts = hub.counts,
            languages = hub.languages,
            savedAtMs = nowMs,
        )
        persist(snap.copy(hubs = hubs))
    }

    fun readList(key: String): ListEntry? = synchronized(lock) {
        val entry = load().lists.orEmpty()[key] ?: return null
        if (entry.savedAtMs <= 0) return null
        if (entry.stations.orEmpty().isEmpty() && entry.others.orEmpty().isEmpty()) return null
        entry
    }

    fun saveList(key: String, page: CatalogPage, nowMs: Long = System.currentTimeMillis()) = synchronized(lock) {
        if (!page.complete) return
        if (page.stations.isEmpty() && page.others.isEmpty()) return
        val snap = load()
        val lists = LinkedHashMap(snap.lists.orEmpty())
        lists[key] = ListEntry(page.stations, page.others, nowMs)
        persist(snap.copy(lists = lists))
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
}
