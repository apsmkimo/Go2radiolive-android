package com.apsmkimo.go2radiolive.domain.station

import java.net.URI
import java.util.Locale

/**
 * Station artwork helpers. The homepage fallback is only that station's own
 * origin (`/favicon.ico`). No third-party favicon or image host is used.
 */
object StationImages {
    fun homepageFavicon(homepage: String): String? {
        val trimmed = homepage.trim()
        if (trimmed.isEmpty()) return null
        val uri = runCatching { URI(trimmed) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: return null
        if (scheme != "http" && scheme != "https") return null
        if (!uri.userInfo.isNullOrBlank()) return null
        val host = uri.host?.trim()?.lowercase(Locale.ROOT) ?: return null
        if (host.isEmpty() || host.contains(" ")) return null
        val port = uri.port
        val defaultPort = if (scheme == "http") 80 else 443
        val portPart = if (port > 0 && port != defaultPort) ":$port" else ""
        return "$scheme://$host$portPart/favicon.ico"
    }

    /** First letter or digit of the name, or "?" when the name has neither. */
    fun placeholderLetter(name: String): String {
        val character = name.trim().firstOrNull { it.isLetterOrDigit() } ?: return "?"
        return character.uppercaseChar().toString()
    }

    fun placeholderColorArgb(name: String): Int {
        val key = name.trim().lowercase(Locale.ROOT)
        var hash = 0
        for (character in key) {
            hash = hash * 31 + character.code
        }
        val index = (hash and Int.MAX_VALUE) % PALETTE.size
        return PALETTE[index]
    }

    val PALETTE: IntArray = intArrayOf(
        0xFF5C6BC0.toInt(),
        0xFF00897B.toInt(),
        0xFFEF6C00.toInt(),
        0xFF8E24AA.toInt(),
        0xFF3949AB.toInt(),
        0xFFC62828.toInt(),
        0xFF2E7D32.toInt(),
        0xFF0277BD.toInt(),
        0xFF6D4C41.toInt(),
        0xFFAD1457.toInt(),
    )
}
