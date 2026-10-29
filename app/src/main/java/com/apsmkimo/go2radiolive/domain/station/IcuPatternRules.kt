package com.apsmkimo.go2radiolive.domain.station

/**
 * Android's `java.util.regex` is ICU. A literal `{` or `}` must be escaped.
 * The desktop JVM accepts a bare `}`, so a pattern can pass host unit tests and
 * then throw [java.util.regex.PatternSyntaxException] while the app is starting.
 */
internal object IcuPatternRules {
    fun compile(pattern: String): Regex? = runCatching { Regex(pattern) }.getOrNull()

    /** Indexes of `{` / `}` that ICU treats as a syntax error. */
    fun illegalBraceIndexes(pattern: String): List<Int> {
        val illegal = ArrayList<Int>()
        var index = 0
        var inClass = false
        while (index < pattern.length) {
            val character = pattern[index]
            if (character == '\\') {
                index += if (index + 1 < pattern.length) 2 else 1
                continue
            }
            if (!inClass && character == '[') {
                inClass = true
                index++
                continue
            }
            if (inClass && character == ']') {
                inClass = false
                index++
                continue
            }
            if (!inClass && character == '{') {
                val close = closingBrace(pattern, index)
                if (close < 0 || !allowedBraceGroup(pattern, index, close)) {
                    illegal.add(index)
                }
                index = if (close < 0) index + 1 else close + 1
                continue
            }
            if (!inClass && character == '}') {
                illegal.add(index)
                index++
                continue
            }
            index++
        }
        return illegal
    }

    private fun closingBrace(pattern: String, open: Int): Int {
        val close = pattern.indexOf('}', startIndex = open + 1)
        if (close < 0) return -1
        if (pattern.substring(open + 1, close).contains('{')) return -1
        return close
    }

    private fun allowedBraceGroup(pattern: String, open: Int, close: Int): Boolean {
        val body = pattern.substring(open + 1, close)
        if (quantifierBody(body)) return true
        if (open >= 2 && pattern[open - 1] == 'p' && pattern[open - 2] == '\\') return true
        if (open >= 2 && pattern[open - 1] == 'P' && pattern[open - 2] == '\\') return true
        return false
    }

    private fun quantifierBody(body: String): Boolean {
        if (body.isEmpty() || !body[0].isDigit()) return false
        var index = 0
        while (index < body.length && body[index].isDigit()) index++
        if (index == body.length) return true
        if (body[index] != ',') return false
        index++
        while (index < body.length && body[index].isDigit()) index++
        return index == body.length
    }
}
