package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// SMCPKG_SUPPORT>>>Cursor115
internal const val PODCAST_SEARCH_LIMIT = 200

/** iTunes Search URL. A null or blank [country] omits the storefront (Apple defaults to the US). */
internal fun itunesPodcastSearchUrl(term: String, country: String?): String {
    val encoded = URLEncoder.encode(term.trim(), StandardCharsets.UTF_8.name())
    val url = StringBuilder("https://itunes.apple.com/search?media=podcast&entity=podcast&term=")
        .append(encoded)
        .append("&limit=")
        .append(PODCAST_SEARCH_LIMIT)
    val code = country?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
    if (code != null) {
        url.append("&country=").append(code)
    }
    return url.toString()
}

/**
 * Maps an iTunes Search payload to shows that can play through the RSS parser.
 * Rows without a feed URL are skipped. Malformed JSON throws.
 */
internal fun mapItunesPodcastSearch(body: String): List<PodcastShow> {
    val root = JsonParser.parseString(body)
    if (!root.isJsonObject) throw JsonSyntaxException("podcast search payload is not an object")
    val results = root.asJsonObject.get("results") ?: return emptyList()
    if (!results.isJsonArray) return emptyList()
    val seen = HashSet<String>()
    val shows = ArrayList<PodcastShow>()
    for (element in results.asJsonArray) {
        if (!element.isJsonObject) continue
        val item = element.asJsonObject
        val feedUrl = item.text("feedUrl")
        if (feedUrl.isEmpty()) continue
        val id = item.text("collectionId").ifEmpty { item.text("trackId") }
        val name = item.text("collectionName").ifEmpty { item.text("trackName") }
        if (id.isEmpty() || name.isEmpty()) continue
        if (!seen.add(id)) continue
        val artwork = item.text("artworkUrl600")
            .ifEmpty { item.text("artworkUrl100") }
            .ifEmpty { item.text("artworkUrl60") }
        shows.add(
            PodcastShow(
                id = id,
                name = name,
                author = item.text("artistName"),
                artworkUrl = artwork,
                feedUrl = feedUrl,
            ),
        )
        if (shows.size >= PODCAST_SEARCH_LIMIT) break
    }
    return shows
}

private fun JsonObject.text(name: String): String {
    val element = get(name) ?: return ""
    return element.asLooseString()
}

private fun JsonElement.asLooseString(): String {
    if (isJsonNull || !isJsonPrimitive) return ""
    val primitive = asJsonPrimitive
    val raw = when {
        primitive.isString || primitive.isNumber -> primitive.asString
        else -> return ""
    }
    return raw.trim()
}
// SMCPKG_SUPPORT<<<Cursor115
