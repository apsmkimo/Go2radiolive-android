package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// SMCPKG_SUPPORT>>>Cursor122
/** First screen size. The RSS parser used to stop here, so older episodes never appeared. */
internal const val PODCAST_QUICK_EPISODE_COUNT = 100

/** Safety cap so a broken feed cannot grow without bound. Real shows are far below this. */
internal const val PODCAST_MAX_EPISODES = 20_000

/**
 * RSS order is newest-first. A feed that publishes dates oldest-first is flipped once,
 * and only when every item has a date, so a partial screen does not reshuffle.
 */
internal fun newestFirst(episodes: List<RadioStation>): List<RadioStation> {
    if (episodes.size < 2) return episodes
    if (episodes.any { it.publishedAtMs <= 0L }) return episodes
    var previous = episodes.first().publishedAtMs
    var oldestFirst = false
    for (index in 1 until episodes.size) {
        val stamp = episodes[index].publishedAtMs
        if (stamp > previous) {
            oldestFirst = true
            break
        }
        previous = stamp
    }
    if (!oldestFirst) return episodes
    return episodes.sortedByDescending { it.publishedAtMs }
}

/**
 * Puts [incoming] first (RSS order), drops a later copy that shares a guid or enclosure URL,
 * and keeps episodes that were only in [existing] so a short read cannot wipe the cache.
 */
internal fun mergePodcastEpisodes(
    existing: List<RadioStation>,
    incoming: List<RadioStation>,
): List<RadioStation> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty() && !hasDuplicateEpisode(incoming)) return incoming
    val byUuid = HashMap<String, Int>(existing.size + incoming.size)
    val byUrl = HashMap<String, Int>(existing.size + incoming.size)
    val out = ArrayList<RadioStation>(existing.size + incoming.size)
    fun remember(index: Int, episode: RadioStation) {
        if (episode.stationUuid.isNotEmpty()) byUuid[episode.stationUuid] = index
        val url = episode.streamUrl.trim()
        if (url.isNotEmpty()) byUrl[url] = index
        val raw = episode.stationUuid.removePrefix("podcast:")
        if (raw.isNotEmpty() && raw != episode.stationUuid) byUuid["podcast:$raw"] = index
    }
    fun find(episode: RadioStation): Int? {
        byUuid[episode.stationUuid]?.let { return it }
        val url = episode.streamUrl.trim()
        if (url.isNotEmpty()) byUrl[url]?.let { return it }
        val raw = episode.stationUuid.removePrefix("podcast:")
        if (raw.isNotEmpty()) {
            byUuid["podcast:$raw"]?.let { return it }
            byUrl[raw]?.let { return it }
        }
        return null
    }
    fun push(episode: RadioStation) {
        if (episode.name.isBlank() || episode.streamUrl.isBlank() || episode.stationUuid.isBlank()) return
        val found = find(episode)
        if (found == null) {
            remember(out.size, episode)
            out.add(episode)
            return
        }
        val current = out[found]
        val currentRaw = current.stationUuid.removePrefix("podcast:")
        val episodeRaw = episode.stationUuid.removePrefix("podcast:")
        val currentIsUrl = currentRaw == current.streamUrl.trim()
        val episodeIsUrl = episodeRaw == episode.streamUrl.trim()
        if (currentIsUrl && !episodeIsUrl) {
            val preferred = current.copy(stationUuid = episode.stationUuid)
            out[found] = preferred
            remember(found, preferred)
        }
    }
    for (episode in incoming) push(episode)
    for (episode in existing) push(episode)
    return out
}

internal fun displayedPodcastEpisodes(
    episodes: List<RadioStation>,
    newestFirst: Boolean,
): List<RadioStation> {
    if (newestFirst || episodes.size < 2) return episodes
    return episodes.asReversed()
}

internal fun parsePodcastDate(raw: String): Long {
    val text = raw.trim()
    if (text.isEmpty()) return 0L
    for (formatter in PODCAST_DATE_FORMATTERS) {
        val zoned = runCatching { ZonedDateTime.parse(text, formatter).toInstant().toEpochMilli() }.getOrNull()
        if (zoned != null) return zoned
        val offset = runCatching { OffsetDateTime.parse(text, formatter).toInstant().toEpochMilli() }.getOrNull()
        if (offset != null) return offset
    }
    return 0L
}

private fun hasDuplicateEpisode(episodes: List<RadioStation>): Boolean {
    if (episodes.size < 2) return false
    val uuids = HashSet<String>(episodes.size)
    val urls = HashSet<String>(episodes.size)
    for (episode in episodes) {
        if (!uuids.add(episode.stationUuid)) return true
        val url = episode.streamUrl.trim()
        if (url.isNotEmpty() && !urls.add(url)) return true
    }
    return false
}

private val PODCAST_DATE_PATTERNS = arrayOf(
    "EEE, dd MMM yyyy HH:mm:ss Z",
    "EEE, dd MMM yyyy HH:mm:ss z",
    "EEE, d MMM yyyy HH:mm:ss Z",
    "EEE, d MMM yyyy HH:mm:ss z",
    "dd MMM yyyy HH:mm:ss Z",
    "d MMM yyyy HH:mm:ss Z",
)

private val PODCAST_DATE_FORMATTERS = PODCAST_DATE_PATTERNS.map { pattern ->
    DateTimeFormatter.ofPattern(pattern, Locale.US)
}
// SMCPKG_SUPPORT<<<Cursor122
