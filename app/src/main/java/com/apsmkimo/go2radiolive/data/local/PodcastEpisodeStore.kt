package com.apsmkimo.go2radiolive.data.local

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import java.io.File

// SMCPKG_SUPPORT>>>Cursor122
/**
 * Parsed episodes for one podcast, so the next visit can paint the full list
 * before the RSS feed is fetched again. One file per show keeps a large feed
 * from rewriting every other show.
 */
class PodcastEpisodeStore(
    private val directory: File,
    private val gson: Gson,
) {
    private data class Payload(val episodes: List<RadioStation>? = emptyList())

    fun read(showId: String): List<RadioStation> {
        val file = fileFor(showId) ?: return emptyList()
        if (!file.isFile) return emptyList()
        val parsed = runCatching { gson.fromJson(file.readText(), Payload::class.java) }.getOrNull()
        return parsed?.episodes.orEmpty().filter { episode ->
            episode.stationUuid.isNotBlank() && episode.name.isNotBlank() && episode.streamUrl.isNotBlank()
        }
    }

    fun save(showId: String, episodes: List<RadioStation>) {
        val file = fileFor(showId) ?: return
        if (episodes.isEmpty()) return
        val parent = file.parentFile
        if (parent != null && !parent.exists()) parent.mkdirs()
        val tmp = File(parent, "${file.name}.tmp")
        tmp.writeText(gson.toJson(Payload(episodes)))
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText())
            tmp.delete()
        }
    }

    private fun fileFor(showId: String): File? {
        val trimmed = showId.trim()
        if (trimmed.isEmpty()) return null
        val safe = buildString(trimmed.length.coerceAtMost(96)) {
            var count = 0
            for (ch in trimmed) {
                if (count == 96) break
                append(if (ch.isLetterOrDigit() || ch == '.' || ch == '_' || ch == '-') ch else '_')
                count++
            }
        }
        if (safe.isEmpty()) return null
        return File(directory, "$safe.json")
    }
}
// SMCPKG_SUPPORT<<<Cursor122
