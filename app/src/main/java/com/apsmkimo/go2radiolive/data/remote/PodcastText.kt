package com.apsmkimo.go2radiolive.data.remote

// SMCPKG_SUPPORT>>>Cursor117
/**
 * Turns podcast HTML into readable plain text. Show notes often arrive as tags
 * or escaped tags; the episode list shows this string in full.
 */
internal fun plainPodcastText(raw: String): String {
    if (raw.isBlank()) return ""
    var text = decodeEntities(decodeEntities(raw))
    text = text.replace(SCRIPT_OR_STYLE, " ")
    text = text.replace(LINE_BREAK, "\n")
    text = text.replace(BLOCK_END, "\n")
    text = text.replace(LIST_ITEM, "\n")
    text = text.replace(ANY_TAG, "")
    text = decodeEntities(text)
    text = text.replace('\u00a0', ' ')
    text = text.replace(SPACES, " ")
    text = text.replace(SPACE_AROUND_NEWLINE, "\n")
    text = text.replace(BLANK_LINES, "\n\n")
    return text.trim()
}

/** Prefer the fullest plain-text notes among the usual RSS fields. */
internal fun pickEpisodeSummary(description: String, summary: String, encoded: String): String {
    return listOf(description, summary, encoded)
        .map { plainPodcastText(it) }
        .filter { it.isNotEmpty() }
        .maxByOrNull { it.length }
        .orEmpty()
}

private fun decodeEntities(raw: String): String {
    if (!raw.contains('&')) return raw
    return ENTITY.replace(raw) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x", ignoreCase = true) -> codePoint(body.drop(2), 16) ?: match.value
            body.startsWith("#") -> codePoint(body.drop(1), 10) ?: match.value
            else -> NAMED_ENTITIES[body] ?: match.value
        }
    }
}

private fun codePoint(digits: String, radix: Int): String? {
    val value = digits.toIntOrNull(radix) ?: return null
    if (value < 0 || value > 0x10FFFF || value == 0xFFFE || value == 0xFFFF) return null
    return String(Character.toChars(value))
}

private val NAMED_ENTITIES = mapOf(
    "amp" to "&",
    "lt" to "<",
    "gt" to ">",
    "quot" to "\"",
    "apos" to "'",
    "nbsp" to " ",
    "hellip" to "…",
    "mdash" to "—",
    "ndash" to "–",
    "rsquo" to "’",
    "lsquo" to "‘",
    "rdquo" to "”",
    "ldquo" to "“",
    "bull" to "•",
)

private val ENTITY = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z][a-zA-Z0-9]+);")
private val SCRIPT_OR_STYLE = Regex("(?is)<(script|style)\\b[^>]*>.*?</\\1>")
private val LINE_BREAK = Regex("(?i)<\\s*br\\s*/?\\s*>")
private val BLOCK_END = Regex("(?i)</\\s*(p|div|li|h[1-6]|tr|blockquote)\\s*>")
private val LIST_ITEM = Regex("(?i)<\\s*li\\b[^>]*>")
private val ANY_TAG = Regex("<[^>]+>")
private val SPACES = Regex("[\\t\\x0B\\f\\r ]+")
private val SPACE_AROUND_NEWLINE = Regex(" *\\n *")
private val BLANK_LINES = Regex("\\n{3,}")
// SMCPKG_SUPPORT<<<Cursor117
