package com.apsmkimo.go2radiolive.ads

/**
 * Gates for an AdMob app-open ad. Pure so unit tests can cover them without a device.
 * Frequency and expiry are both four hours. [lastShownMs] of 0 means the ad has never shown.
 */
object AppOpenPolicy {
    const val MIN_INTERVAL_MS = 4L * 60L * 60L * 1000L
    const val EXPIRY_MS = 4L * 60L * 60L * 1000L
    const val COLD_START_TIMEOUT_MS = 4_000L

    fun isAdExpired(nowMs: Long, adLoadedAtMs: Long): Boolean {
        if (adLoadedAtMs <= 0L) return true
        return nowMs - adLoadedAtMs >= EXPIRY_MS
    }

    fun canShowAppOpen(
        nowMs: Long,
        lastShownMs: Long,
        adLoadedAtMs: Long,
        audioActive: Boolean,
        consentFormVisible: Boolean,
        fullScreenAdVisible: Boolean,
    ): Boolean {
        if (audioActive || consentFormVisible || fullScreenAdVisible) return false
        if (isAdExpired(nowMs, adLoadedAtMs)) return false
        if (lastShownMs > 0L && nowMs - lastShownMs < MIN_INTERVAL_MS) return false
        return true
    }
}
