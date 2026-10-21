package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.model.CatalogLanguage
import com.apsmkimo.go2radiolive.domain.model.CatalogSort
import com.apsmkimo.go2radiolive.domain.model.CountResult
import com.apsmkimo.go2radiolive.domain.model.RadioStation

/**
 * Fixed category ids and the Radio-Browser tag synonyms used to find them.
 * Counts and lists query these tags. They do not download the station database.
 */
internal object CatalogTopics {
    const val GLOBAL = "GLOBAL"
    const val OTHER_THRESHOLD = 10
    const val LIST_LIMIT = 80
    const val TAG_LIMIT = 40
    const val RANK_LIMIT = 30
    const val LANGUAGE_SAMPLE = 200
    const val HIGH_BITRATE = 128

    const val POP = "pop"
    const val NEWS_TALK = "news_talk"
    const val MUSIC = "music"
    const val DECADE = "decade"
    const val LANGUAGE = "language"

    const val NEWS = "news"
    const val TALK = "talk"
    const val SPORTS = "sports"
    const val RELIGIOUS = "religious"
    const val CLASSICAL = "classical"
    const val JAZZ = "jazz"
    const val ROCK = "rock"
    const val ELECTRONIC = "electronic"
    const val HIPHOP = "hiphop"
    const val COUNTRY = "country"
    const val LOFI = "lofi"
    const val WORLD = "world"
    const val D60 = "d60"
    const val D70 = "d70"
    const val D80 = "d80"
    const val D90 = "d90"
    const val D2000 = "d2000"
    const val OLDIES = "oldies"

    const val RANK_VOTES = "rank:votes"
    const val RANK_CLICKS = "rank:clicks"
    const val RANK_TREND = "rank:trend"

    val hubCards = listOf(POP, NEWS_TALK, MUSIC, DECADE, LANGUAGE)

    fun topicIds(): List<String> = tags.keys.toList()

    private val tags: Map<String, List<String>> = mapOf(
        POP to listOf("pop", "流行", "流行音樂", "流行音乐", "ポップ", "top 40"),
        NEWS to listOf("news", "新聞", "新闻", "ニュース"),
        TALK to listOf("talk", "談話", "谈话", "トーク"),
        SPORTS to listOf("sports", "sport", "體育", "体育", "スポーツ"),
        RELIGIOUS to listOf("religious", "religion", "宗教", "gospel"),
        CLASSICAL to listOf("classical", "古典", "古典音樂", "古典音乐", "クラシック"),
        JAZZ to listOf("jazz", "爵士", "ジャズ"),
        ROCK to listOf("rock", "搖滾", "摇滚", "ロック"),
        ELECTRONIC to listOf("electronic", "dance", "電子", "电子", "エレクトロ"),
        HIPHOP to listOf("hip hop", "hiphop", "r&b", "嘻哈", "ヒップホップ"),
        COUNTRY to listOf("country", "鄉村", "乡村", "カントリー"),
        LOFI to listOf("lofi", "easy listening", "lounge", "chill", "輕音樂", "轻音乐", "ローファイ"),
        WORLD to listOf("world music", "world", "世界音樂", "世界音乐", "ワールド"),
        D60 to listOf("60s", "1960s", "60年代"),
        D70 to listOf("70s", "1970s", "70年代"),
        D80 to listOf("80s", "1980s", "80年代"),
        D90 to listOf("90s", "1990s", "90年代"),
        D2000 to listOf("2000s", "00s", "2000年代"),
        OLDIES to listOf("oldies", "懷舊", "怀旧", "懐メロ"),
    )

    fun children(group: String): List<String> = when (group) {
        NEWS_TALK -> listOf(NEWS, TALK, SPORTS, RELIGIOUS)
        MUSIC -> listOf(CLASSICAL, JAZZ, ROCK, ELECTRONIC, HIPHOP, COUNTRY, LOFI, WORLD)
        DECADE -> listOf(D60, D70, D80, D90, D2000, OLDIES)
        else -> emptyList()
    }

    fun isGroup(id: String): Boolean = id == NEWS_TALK || id == MUSIC || id == DECADE || id == LANGUAGE

    fun searchTags(topic: String): List<String> = tags[topic].orEmpty()

    /** Fewer tags for the hub count, still covering English and CJK spellings. */
    fun countTags(topic: String): List<String> = searchTags(topic).take(4)

    fun countFromDirectory(topic: String, directory: Map<String, Int>): CountResult {
        val total = searchTags(topic).sumOf { tag -> directory[tag.lowercase()] ?: 0 }
        return CountResult(count = total, capped = false)
    }

    fun parentCount(group: String, counts: Map<String, CountResult>): CountResult? {
        val kids = children(group).map { counts[it] ?: return null }
        if (kids.isEmpty()) return null
        return CountResult(
            count = kids.sumOf { it.count },
            capped = kids.any { it.capped },
        )
    }

    fun needsOtherCountries(localCount: Int): Boolean = localCount < OTHER_THRESHOLD

    fun mergeStations(
        groups: List<List<RadioStation>>,
        sort: CatalogSort,
        limit: Int = LIST_LIMIT,
    ): List<RadioStation> {
        val seen = HashSet<String>()
        // Capacity follows the stations actually in hand. `limit` only stops the loop,
        // so a caller cannot ask for an Int.MAX_VALUE array.
        val capacity = if (limit <= 0) {
            0
        } else {
            groups.sumOf { it.size }.coerceAtMost(limit)
        }
        val out = ArrayList<RadioStation>(capacity)
        for (station in sortStations(groups.flatten(), sort)) {
            if (station.stationUuid.isBlank() || !seen.add(station.stationUuid)) continue
            out.add(station)
            if (out.size >= limit) break
        }
        return out
    }

    fun sortStations(stations: List<RadioStation>, sort: CatalogSort): List<RadioStation> {
        return when (sort) {
            CatalogSort.CLICKS -> stations.sortedByDescending { it.clickCount }
            CatalogSort.VOTES -> stations.sortedByDescending { it.votes }
            CatalogSort.NAME -> stations.sortedBy { it.name.lowercase() }
        }
    }

    fun matchesLanguage(station: RadioStation, language: String): Boolean {
        if (language.isBlank()) return true
        return station.language.split(',').any { it.trim().equals(language, ignoreCase = true) }
    }

    fun applyFilters(
        stations: List<RadioStation>,
        language: String,
        highBitrate: Boolean,
    ): List<RadioStation> {
        return stations.filter { station ->
            matchesLanguage(station, language) &&
                (!highBitrate || station.bitrate >= HIGH_BITRATE)
        }
    }

    fun usefulLanguage(name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.length < 2 || trimmed.length > 32) return false
        if (trimmed.first() == '#' || trimmed.first() == '\'' || trimmed.first() == '"') return false
        return trimmed.any { it.isLetter() }
    }

    fun tallyLanguages(stations: List<RadioStation>): List<CatalogLanguage> {
        val counts = LinkedHashMap<String, Int>()
        for (station in stations) {
            station.language.split(',')
                .map { it.trim() }
                .filter { usefulLanguage(it) }
                .map { it.lowercase() }
                .distinct()
                .forEach { name -> counts[name] = (counts[name] ?: 0) + 1 }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { CatalogLanguage(name = it.key, stationCount = it.value) }
    }

    fun listKey(scope: String, kind: String, sort: CatalogSort, language: String, highBitrate: Boolean): String {
        val quality = if (highBitrate) HIGH_BITRATE else 0
        return "$scope|$kind|$sort|$language|$quality"
    }
}
