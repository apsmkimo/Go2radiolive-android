package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import java.net.URI
import java.util.Locale

fun interface PlaylistBodySource {
    /** Playlist text, or null when it cannot be read. Blocking, bounded by [timeoutMs]. */
    fun fetch(url: String, timeoutMs: Long): String?
}

object PlaylistParser {
    private const val MAX_ENTRIES = 8

    fun isPlaylistUrl(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        return path.endsWith(".pls") ||
            path.endsWith(".m3u") ||
            path.endsWith(".m3u8") ||
            path.endsWith(".asx") ||
            path.endsWith(".ram")
    }

    fun parse(body: String, baseUrl: String): List<String> {
        val text = body.trim().removePrefix("\uFEFF")
        if (text.isEmpty() || text.contains('\u0000')) return emptyList()
        val found = when {
            text.contains("#EXT-X-", ignoreCase = true) ->
                parseM3u(text, baseUrl).filterNot { isHlsSegment(it) || isPlaylistUrl(it) }
            fileLine?.containsMatchIn(text) == true || text.contains("[playlist]", ignoreCase = true) ->
                parsePls(text)
            text.contains("<asx", ignoreCase = true) || text.contains("<ref", ignoreCase = true) ->
                parseAsx(text)
            else -> parseM3u(text, baseUrl)
        }
        return found.filter { isHttp(it) }.distinct().take(MAX_ENTRIES)
    }

    fun playbackCandidates(station: RadioStation, entryUrl: String?): List<String> {
        val raw = ArrayList<String>()
        if (!entryUrl.isNullOrBlank()) raw.add(entryUrl.trim())
        if (station.streamUrl.isNotBlank()) raw.add(station.streamUrl.trim())
        station.alternateUrls.orEmpty().forEach { url ->
            if (url.isNotBlank()) raw.add(url.trim())
        }
        val seen = LinkedHashSet<String>()
        for (url in raw) {
            if (isHttp(url)) seen.add(url)
            if (seen.size >= MAX_ENTRIES) break
        }
        return seen.toList()
    }

    fun isHttp(url: String): Boolean {
        val scheme = runCatching { URI(url.trim()).scheme }.getOrNull()?.lowercase(Locale.ROOT)
        return scheme == "http" || scheme == "https"
    }

    private fun parsePls(text: String): List<String> {
        val pattern = fileLine ?: return emptyList()
        return pattern.findAll(text).map { it.groupValues[1].trim() }.toList()
    }

    private fun parseM3u(text: String, baseUrl: String): List<String> {
        val out = ArrayList<String>()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            out.add(resolve(baseUrl, trimmed))
        }
        return out
    }

    private fun parseAsx(text: String): List<String> {
        val pattern = href ?: return emptyList()
        return pattern.findAll(text).map { it.groupValues[1].trim() }.toList()
    }

    private fun resolve(baseUrl: String, reference: String): String {
        if (isHttp(reference)) return reference
        return runCatching { URI(baseUrl).resolve(reference).toString() }.getOrDefault(reference)
    }

    private fun isHlsSegment(url: String): Boolean {
        val path = url.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        return path.endsWith(".ts") || path.endsWith(".m4s") || path.endsWith(".cmfv") || path.endsWith(".cmfa")
    }

    // SMCPKG_SUPPORT>>>Cursor120
    // private val FILE_LINE = Regex("""(?im)^\s*File\d+\s*=\s*(\S.*?)\s*$""")
    // private val HREF = Regex("""(?i)href\s*=\s*["']([^"']+)["']""")
    internal const val FILE_LINE_PATTERN = """(?im)^\s*File\d+\s*=\s*(\S.*?)\s*$"""
    internal const val HREF_PATTERN = """(?i)href\s*=\s*["']([^"']+)["']"""
    internal val patternSources: List<String> = listOf(FILE_LINE_PATTERN, HREF_PATTERN)
    private val fileLine: Regex? by lazy { IcuPatternRules.compile(FILE_LINE_PATTERN) }
    private val href: Regex? by lazy { IcuPatternRules.compile(HREF_PATTERN) }
    // SMCPKG_SUPPORT<<<Cursor120
}
