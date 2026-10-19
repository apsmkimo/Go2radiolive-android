package com.apsmkimo.go2radiolive.data.remote

import android.util.Xml
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/**
 * Reads podcast items and their audio enclosures. Each episode becomes a
 * [RadioStation] with [RadioStation.directStream] so playback skips Radio-Browser.
 */
internal object PodcastRssParser {
    fun parse(
        stream: InputStream,
        showName: String,
        artworkUrl: String,
        limit: Int = 100,
    ): List<RadioStation> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)
        val episodes = ArrayList<RadioStation>(limit.coerceAtMost(100))
        var inItem = false
        var title = ""
        var guid = ""
        var audioUrl = ""
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < limit) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = localName(parser.name)
                    if (name == "item") {
                        inItem = true
                        title = ""
                        guid = ""
                        audioUrl = ""
                    } else if (inItem && name == "title" && title.isEmpty()) {
                        title = parser.nextText().trim()
                    } else if (inItem && name == "guid" && guid.isEmpty()) {
                        guid = parser.nextText().trim()
                    } else if (inItem && audioUrl.isEmpty() && (name == "enclosure" || name == "content")) {
                        val url = parser.getAttributeValue(null, "url").orEmpty().trim()
                        val type = parser.getAttributeValue(null, "type").orEmpty().lowercase()
                        if (url.isNotEmpty() && isAudio(type, url)) audioUrl = url
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (localName(parser.name) == "item" && inItem) {
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
                                ),
                            )
                        }
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

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
