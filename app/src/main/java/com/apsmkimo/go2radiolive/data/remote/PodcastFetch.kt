package com.apsmkimo.go2radiolive.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/** A saved chart older than this is refreshed in the background. */
internal const val PODCAST_CACHE_MAX_AGE_MS = 12L * 60L * 60L * 1000L

internal fun isPodcastCacheStale(savedAtMs: Long, nowMs: Long): Boolean {
    return nowMs - savedAtMs >= PODCAST_CACHE_MAX_AGE_MS
}

/**
 * Runs [block] up to [attempts] times (the first try plus retries).
 * Backs off between tries. [CancellationException] is not retried.
 */
internal suspend fun <T> retryIo(
    attempts: Int = 3,
    initialDelayMs: Long = 300,
    // SMCPKG_SUPPORT>>>Cursor107
    // block: () -> T,
    block: suspend () -> T,
    // SMCPKG_SUPPORT<<<Cursor107
): T {
    var delayMs = initialDelayMs
    var last: Exception? = null
    val tries = attempts.coerceAtLeast(1)
    for (index in 0 until tries) {
        try {
            return block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            last = error
            if (index == tries - 1) break
            delay(delayMs)
            delayMs = (delayMs * 2).coerceAtMost(2_000L)
        }
    }
    throw last ?: IOException("request failed")
}
