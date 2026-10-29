package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import java.net.URI
import java.time.Instant
import java.util.Locale

/**
 * Prepares radio lists: drop stations Radio-Browser already marks unusable,
 * merge duplicate registrations, and rank the local tab.
 * Podcast episodes (directStream) stay in their original order.
 */
object StationLists {
    /** A successful check inside this window lifts a local-tab station. */
    const val FRESH_CHECK_MS: Long = 14L * 24 * 60 * 60 * 1000

    fun present(
        stations: List<RadioStation>,
        failures: Map<String, PlayFailureRecord>,
        nowMs: Long,
        rankLocal: Boolean,
    ): List<RadioStation> {
        val deduped = dedupe(stations)
        val ranked = if (rankLocal) {
            deduped.sortedByDescending { localRankScore(it, nowMs) }
        } else {
            deduped
        }
        return StationFailurePolicy.sink(ranked, failures, nowMs)
    }

    fun dedupe(stations: List<RadioStation>): List<RadioStation> {
        if (stations.isEmpty()) return stations
        val radioIndexes = ArrayList<Int>()
        for (index in stations.indices) {
            val station = stations[index]
            if (!station.directStream && isListable(station)) radioIndexes.add(index)
        }
        val count = radioIndexes.size
        if (count == 0) {
            return stations.filter { it.directStream }
        }
        val parent = IntArray(count) { it }
        val byStream = HashMap<String, Int>()
        val byName = HashMap<String, Int>()
        for (local in 0 until count) {
            val station = stations[radioIndexes[local]]
            for (key in streamClusterKeys(station)) {
                val previous = byStream[key]
                if (previous == null) byStream[key] = local else union(parent, previous, local)
            }
            val name = nameClusterKey(station)
            if (name != null) {
                val previous = byName[name]
                if (previous == null) byName[name] = local else union(parent, previous, local)
            }
        }
        val groups = HashMap<Int, MutableList<Int>>()
        for (local in 0 until count) {
            groups.getOrPut(find(parent, local)) { mutableListOf() }.add(local)
        }
        val localOf = HashMap<Int, Int>(count)
        radioIndexes.forEachIndexed { local, original -> localOf[original] = local }
        val emitted = HashSet<Int>()
        val out = ArrayList<RadioStation>(stations.size)
        for (index in stations.indices) {
            val station = stations[index]
            if (station.directStream) {
                out.add(station)
                continue
            }
            val local = localOf[index] ?: continue
            val root = find(parent, local)
            if (!emitted.add(root)) continue
            val members = groups.getValue(root).map { stations[radioIndexes[it]] }
            out.add(represent(members))
        }
        return out
    }

    /**
     * Local tab score. Clicks stay the main signal. Votes add a smaller lift,
     * and a successful check within [FRESH_CHECK_MS] adds about twenty clicks
     * so a recently verified station outranks an equally popular stale one.
     */
    fun localRankScore(station: RadioStation, nowMs: Long): Long {
        val clicks = station.clickCount.coerceAtLeast(0).toLong()
        val votes = station.votes.coerceAtLeast(0).toLong()
        val fresh = if (recentlyVerified(station, nowMs)) 1L else 0L
        return clicks * 1_000L + votes * 800L + fresh * 20_000L
    }

    fun recentlyVerified(station: RadioStation, nowMs: Long): Boolean {
        if (station.lastCheckOk != 1) return false
        if (station.lastCheckTimeMs <= 0L) return false
        val age = nowMs - station.lastCheckTimeMs
        return age in 0..FRESH_CHECK_MS
    }

    fun isListable(station: RadioStation): Boolean {
        if (station.directStream) return true
        val hasStream = station.streamUrl.isNotBlank() ||
            station.alternateUrls.orEmpty().any { it.isNotBlank() }
        if (!hasStream) return false
        if (station.lastCheckOk == 0) return false
        if (station.sslError) return false
        if (station.hls) return false
        if (!codecPlayable(station.codec)) return false
        return true
    }

    fun codecPlayable(codec: String?): Boolean {
        val raw = codec?.trim().orEmpty()
        if (raw.isEmpty()) return true
        val normalized = raw.uppercase(Locale.ROOT)
        if (normalized == "UNKNOWN" || normalized == "NONE" || normalized == "N/A") return true
        if (normalized == "RA" || normalized == "RM" || normalized == "RAM" || normalized == "WV") return false
        return BLOCKED_CODEC.none { token ->
            normalized == token ||
                normalized.startsWith("$token ") ||
                normalized.startsWith("$token/") ||
                normalized.startsWith("$token-") ||
                normalized.startsWith("${token}_") ||
                normalized.contains(token)
        }
    }

    fun parseCheckTime(raw: String?): Long? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        runCatching { Instant.parse(text).toEpochMilli() }.getOrNull()?.let { return it }
        val normalized = text.replace(' ', 'T')
        val zoned = when {
            normalized.endsWith("Z", ignoreCase = true) -> normalized
            offsetRegex?.containsMatchIn(normalized) == true -> normalized
            else -> normalized + "Z"
        }
        return runCatching { Instant.parse(zoned).toEpochMilli() }.getOrNull()
    }

    fun nameClusterKey(station: RadioStation): String? {
        val name = nameKey(station.name) ?: return null
        return name + "|" + regionKey(station)
    }

    fun nameKey(name: String): String? {
        var text = name.lowercase(Locale.ROOT)
        // SMCPKG_SUPPORT>>>Cursor120
        // text = BRACKETS.replace(text, " ")
        // text = FREQUENCY.replace(text, " ")
        // val tokens = text.split(NON_WORD).filter { it.isNotBlank() && it !in DROP_TOKENS }
        text = bracketsRegex?.replace(text, " ") ?: text
        text = frequencyRegex?.replace(text, " ") ?: text
        val tokens = text.split(nonWordRegex).filter { it.isNotBlank() && it !in DROP_TOKENS }
        // SMCPKG_SUPPORT<<<Cursor120
        if (tokens.isEmpty()) return null
        var core = tokens
        while (core.size > 1 && (core.first() == "radio" || core.first() == "the")) {
            core = core.drop(1)
        }
        while (core.size > 1 && core.last() == "radio") {
            core = core.dropLast(1)
        }
        if (core.size == 1 && core[0] in GENERIC_SINGLE) return null
        val key = core.joinToString(" ")
        if (key.replace(" ", "").length < 3) return null
        return key
    }

    fun streamClusterKeys(station: RadioStation): List<String> {
        val urls = ArrayList<String>(1 + station.alternateUrls.orEmpty().size)
        if (station.streamUrl.isNotBlank()) urls.add(station.streamUrl)
        station.alternateUrls.orEmpty().forEach { url ->
            if (url.isNotBlank()) urls.add(url)
        }
        return urls.mapNotNull { specificStreamKey(it) }.distinct()
    }

    fun specificStreamKey(url: String): String? {
        val uri = runCatching { URI(url.trim()) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        val host = uri.host?.trim()?.lowercase(Locale.ROOT)?.removePrefix("www.") ?: return null
        if (host.isEmpty()) return null
        val path = (uri.path ?: "").trim().lowercase(Locale.ROOT).trimEnd('/').ifEmpty { "/" }
        if (path in GENERIC_PATHS) return null
        val port = uri.port
        val portPart = if (port > 0 && port != 80 && port != 443) ":$port" else ""
        return host + portPart + path
    }

    private fun represent(members: List<RadioStation>): RadioStation {
        val winner = members.reduce { best, station -> if (prefer(station, best)) station else best }
        val urls = LinkedHashSet<String>()
        val ordered = members.sortedWith { left, right ->
            when {
                prefer(left, right) -> -1
                prefer(right, left) -> 1
                else -> 0
            }
        }
        for (member in ordered) {
            if (member.streamUrl.isNotBlank()) urls.add(member.streamUrl.trim())
            member.alternateUrls.orEmpty().forEach { url ->
                if (url.isNotBlank()) urls.add(url.trim())
            }
        }
        urls.remove(winner.streamUrl.trim())
        val ids = members.map { it.stationUuid }.filter { it.isNotBlank() }.distinct()
        return winner.copy(
            alternateUrls = urls.toList(),
            mergedUuids = if (ids.size > 1) ids else winner.mergedUuids,
        )
    }

    /** True when [candidate] should replace [incumbent] as the visible station. */
    fun prefer(candidate: RadioStation, incumbent: RadioStation): Boolean {
        val candidateOk = if (candidate.lastCheckOk == 1) 1 else 0
        val incumbentOk = if (incumbent.lastCheckOk == 1) 1 else 0
        if (candidateOk != incumbentOk) return candidateOk > incumbentOk
        val candidateIcon = if (candidate.faviconUrl.isNotBlank()) 1 else 0
        val incumbentIcon = if (incumbent.faviconUrl.isNotBlank()) 1 else 0
        if (candidateIcon != incumbentIcon) return candidateIcon > incumbentIcon
        if (candidate.clickCount != incumbent.clickCount) return candidate.clickCount > incumbent.clickCount
        if (candidate.votes != incumbent.votes) return candidate.votes > incumbent.votes
        if (candidate.bitrate != incumbent.bitrate) return candidate.bitrate > incumbent.bitrate
        return false
    }

    private fun regionKey(station: RadioStation): String {
        val code = station.countryCode.trim()
        if (code.isNotEmpty()) return code.uppercase(Locale.ROOT)
        return station.country.trim().lowercase(Locale.ROOT)
    }

    private fun find(parent: IntArray, index: Int): Int {
        var cursor = index
        while (parent[cursor] != cursor) {
            parent[cursor] = parent[parent[cursor]]
            cursor = parent[cursor]
        }
        return cursor
    }

    private fun union(parent: IntArray, left: Int, right: Int) {
        val a = find(parent, left)
        val b = find(parent, right)
        if (a == b) return
        if (a < b) parent[b] = a else parent[a] = b
    }

    // SMCPKG_SUPPORT>>>Cursor120
    // Android ICU rejects a literal unescaped "}". The JVM accepts it, so this
    // used to pass host tests and then crash StationLists' class init on device:
    // private val BRACKETS = Regex("""\([^)]*\)|\[[^\]]*\]|\{[^}]*}|【[^】]*】|「[^」]*」|『[^』]*』""")
    // Patterns are compiled lazily and a syntax error becomes "skip this rule"
    // instead of ExceptionInInitializerError during the first list bind.
    internal const val BRACKETS_PATTERN =
        """\([^)]*\)|\[[^\]]*\]|\{[^}]*\}|【[^】]*】|「[^」]*」|『[^』]*』"""
    internal const val FREQUENCY_PATTERN =
        """(?i)(?<!\d)(?:8[7-9]|9\d|10[0-8])(?:[.,]\d{1,2})?\s*(?:mhz|fm|am)?(?!\d)|(?<!\d)\d{2,4}(?:[.,]\d{1,2})?\s*(?:mhz|khz|fm|am)(?![a-z0-9])"""
    internal const val NON_WORD_PATTERN = """[^\p{L}\p{N}]+"""
    internal const val OFFSET_PATTERN = """[+-]\d{2}:?\d{2}${'$'}"""

    internal val patternSources: List<String> = listOf(
        BRACKETS_PATTERN,
        FREQUENCY_PATTERN,
        NON_WORD_PATTERN,
        OFFSET_PATTERN,
    )

    private val bracketsRegex: Regex? by lazy { IcuPatternRules.compile(BRACKETS_PATTERN) }
    private val frequencyRegex: Regex? by lazy { IcuPatternRules.compile(FREQUENCY_PATTERN) }
    private val nonWordRegex: Regex by lazy { IcuPatternRules.compile(NON_WORD_PATTERN) ?: Regex("""\s+""") }
    private val offsetRegex: Regex? by lazy { IcuPatternRules.compile(OFFSET_PATTERN) }
    // SMCPKG_SUPPORT<<<Cursor120
    private val DROP_TOKENS = setOf(
        "fm", "am", "hd", "hd1", "hd2", "hd3", "live", "online", "stream", "streaming", "webradio",
    )
    private val GENERIC_SINGLE = setOf(
        "radio", "music", "news", "talk", "sport", "sports", "rock", "pop", "jazz", "classical",
        "classic", "gold", "oldies", "love", "star", "hit", "hits", "top", "the", "live", "community",
    )
    private val GENERIC_PATHS = setOf(
        "/", "/stream", "/live", "/radio", "/listen", "/play", "/audio", "/main",
        "/stream.mp3", "/live.mp3", "/radio.mp3", "/audio.mp3",
        "/listen.pls", "/stream.pls", "/stream.m3u", "/live.m3u", "/;",
    )
    private val BLOCKED_CODEC = listOf(
        "WMA", "WMAV1", "WMAV2", "WMAPRO", "ASF", "REALAUDIO", "REAL", "ATRAC", "ATRAC3",
        "APE", "WAVPACK", "SHORTEN", "DSD", "HLS", "DASH", "MUSEPACK", "MPC",
    )
}
