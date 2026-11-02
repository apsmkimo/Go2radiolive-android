package com.apsmkimo.go2radiolive.data.remote

import android.util.Xml
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.InputStream

/**
 * Reads podcast items and their audio enclosures. Each episode becomes a
 * [RadioStation] with [RadioStation.directStream] so playback skips Radio-Browser.
 * The pull parser walks the feed once so a multi-megabyte document is never held as a tree.
 */
internal object PodcastRssParser {
    // SMCPKG_SUPPORT>>>Cursor122
    /** Tests on the JVM replace this. Production uses Android's streaming pull parser. */
    internal var newPullParser: () -> XmlPullParser = { Xml.newPullParser() }
    // SMCPKG_SUPPORT<<<Cursor122

    fun parse(
        stream: InputStream,
        showName: String,
        artworkUrl: String,
        // SMCPKG_SUPPORT>>>Cursor122
        // limit: Int = 100,
        limit: Int = PODCAST_MAX_EPISODES,
        quickCount: Int = 0,
        onQuick: ((List<RadioStation>) -> Unit)? = null,
        parser: XmlPullParser? = null,
        // SMCPKG_SUPPORT<<<Cursor122
    ): List<RadioStation> {
        // SMCPKG_SUPPORT>>>Cursor122
        // val parser = Xml.newPullParser()
        val pull = parser ?: newPullParser()
        // SMCPKG_SUPPORT<<<Cursor122
        pull.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        pull.setInput(stream, null)
        val episodes = ArrayList<RadioStation>()
        var inItem = false
        var title = ""
        var guid = ""
        var audioUrl = ""
        // SMCPKG_SUPPORT>>>Cursor117
        var description = ""
        var summary = ""
        var encoded = ""
        // SMCPKG_SUPPORT<<<Cursor117
        // SMCPKG_SUPPORT>>>Cursor122
        var publishedAtMs = 0L
        var quickSent = false
        // SMCPKG_SUPPORT<<<Cursor122
        var event = pull.eventType
        // SMCPKG_SUPPORT>>>Cursor122
        // while (event != XmlPullParser.END_DOCUMENT && episodes.size < limit) {
        try {
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < limit) {
        // SMCPKG_SUPPORT<<<Cursor122
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = localName(pull.name)
                    if (name == "item") {
                        inItem = true
                        title = ""
                        guid = ""
                        audioUrl = ""
                        // SMCPKG_SUPPORT>>>Cursor117
                        description = ""
                        summary = ""
                        encoded = ""
                        // SMCPKG_SUPPORT<<<Cursor117
                        // SMCPKG_SUPPORT>>>Cursor122
                        publishedAtMs = 0L
                        // SMCPKG_SUPPORT<<<Cursor122
                    } else if (inItem && name == "title" && title.isEmpty()) {
                        // SMCPKG_SUPPORT>>>Cursor117
                        // title = parser.nextText().trim()
                        title = plainPodcastText(readInnerText(pull))
                        // SMCPKG_SUPPORT<<<Cursor117
                    } else if (inItem && name == "guid" && guid.isEmpty()) {
                        // SMCPKG_SUPPORT>>>Cursor117
                        // guid = parser.nextText().trim()
                        guid = readInnerText(pull).trim()
                        // SMCPKG_SUPPORT<<<Cursor117
                    } else if (inItem && audioUrl.isEmpty() && (name == "enclosure" || name == "content")) {
                        val url = pull.getAttributeValue(null, "url").orEmpty().trim()
                        val type = pull.getAttributeValue(null, "type").orEmpty().lowercase()
                        if (url.isNotEmpty() && isAudio(type, url)) audioUrl = url
                    // SMCPKG_SUPPORT>>>Cursor117
                    } else if (inItem && name == "description" && description.isEmpty()) {
                        description = readInnerText(pull)
                    } else if (inItem && name == "summary" && summary.isEmpty()) {
                        summary = readInnerText(pull)
                    } else if (inItem && name == "encoded" && encoded.isEmpty()) {
                        encoded = readInnerText(pull)
                    // SMCPKG_SUPPORT<<<Cursor117
                    // SMCPKG_SUPPORT>>>Cursor122
                    } else if (inItem && name == "pubdate" && publishedAtMs == 0L) {
                        publishedAtMs = parsePodcastDate(readInnerText(pull))
                    // SMCPKG_SUPPORT<<<Cursor122
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (localName(pull.name) == "item" && inItem) {
                        inItem = false
                        if (title.isNotEmpty() && audioUrl.isNotEmpty()) {
                            val id = guid.ifBlank { audioUrl }
                            episodes.add(
                                RadioStation(
                                    stationUuid = "podcast:$id",
                                    name = title,
                                    streamUrl = audioUrl,
                                    faviconUrl = artworkUrl,
                                    country = showName,
                                    tags = "podcast",
                                    directStream = true,
                                    // SMCPKG_SUPPORT>>>Cursor117
                                    summary = pickEpisodeSummary(description, summary, encoded).ifBlank { null },
                                    // SMCPKG_SUPPORT<<<Cursor117
                                    // SMCPKG_SUPPORT>>>Cursor122
                                    publishedAtMs = publishedAtMs,
                                    // SMCPKG_SUPPORT<<<Cursor122
                                ),
                            )
                            // SMCPKG_SUPPORT>>>Cursor122
                            if (!quickSent && quickCount > 0 && onQuick != null && episodes.size >= quickCount) {
                                quickSent = true
                                onQuick(ArrayList(episodes))
                            }
                            // SMCPKG_SUPPORT<<<Cursor122
                        }
                    }
                }
            }
            event = pull.next()
        }
        // SMCPKG_SUPPORT>>>Cursor122
        } catch (malformed: XmlPullParserException) {
            if (episodes.isEmpty()) throw malformed
        }
        // SMCPKG_SUPPORT<<<Cursor122
        return episodes
    }

    // SMCPKG_SUPPORT>>>Cursor117
    /** Reads until the matching end tag and leaves the parser on that end tag. */
    private fun readInnerText(parser: XmlPullParser): String {
        if (parser.isEmptyElementTag) return ""
        val depth = parser.depth
        val text = StringBuilder()
        var event = parser.next()
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.END_TAG && parser.depth == depth) break
            if (event == XmlPullParser.TEXT || event == XmlPullParser.CDSECT) {
                text.append(parser.text ?: "")
            }
            event = parser.next()
        }
        return text.toString()
    }
    // SMCPKG_SUPPORT<<<Cursor117

    private fun localName(raw: String?): String {
        val name = raw?.trim()?.lowercase().orEmpty()
        val colon = name.lastIndexOf(':')
        return if (colon >= 0) name.substring(colon + 1) else name
    }

    private fun isAudio(type: String, url: String): Boolean {
        if (type.startsWith("audio")) return true
        if (type.isEmpty()) return true
        val path = url.lowercase().substringBefore('?')
        return path.endsWith(".mp3") || path.endsWith(".m4a") ||
            path.endsWith(".aac") || path.endsWith(".mp4") || path.endsWith(".ogg")
    }
}
