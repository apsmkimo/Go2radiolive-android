package com.apsmkimo.go2radiolive.ads

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppOpenPolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun showsWhenNeverShownAndTheAdIsFresh() {
        assertTrue(
            AppOpenPolicy.canShowAppOpen(
                nowMs = now,
                lastShownMs = 0L,
                adLoadedAtMs = now - 1_000L,
                audioActive = false,
                consentFormVisible = false,
                fullScreenAdVisible = false,
            ),
        )
    }

    @Test
    fun blocksWhileAudioConsentOrAnotherFullScreenAd() {
        assertFalse(show(audioActive = true))
        assertFalse(show(consentFormVisible = true))
        assertFalse(show(fullScreenAdVisible = true))
    }

    @Test
    fun allowsTheNextShowAtFourHours() {
        val justUnder = now - AppOpenPolicy.MIN_INTERVAL_MS + 1L
        assertFalse(show(lastShownMs = justUnder))
        assertTrue(show(lastShownMs = now - AppOpenPolicy.MIN_INTERVAL_MS))
    }

    @Test
    fun blocksAnExpiredOrMissingAd() {
        assertFalse(show(adLoadedAtMs = now - AppOpenPolicy.EXPIRY_MS))
        assertTrue(show(adLoadedAtMs = now - AppOpenPolicy.EXPIRY_MS + 1L))
        assertFalse(show(adLoadedAtMs = 0L))
        assertTrue(AppOpenPolicy.isAdExpired(now, now - AppOpenPolicy.EXPIRY_MS))
    }

    @Test
    fun coldStartWaitsAtMostFourSeconds() {
        assertTrue(AppOpenPolicy.COLD_START_TIMEOUT_MS == 4_000L)
    }

    private fun show(
        lastShownMs: Long = 0L,
        adLoadedAtMs: Long = now,
        audioActive: Boolean = false,
        consentFormVisible: Boolean = false,
        fullScreenAdVisible: Boolean = false,
    ): Boolean {
        return AppOpenPolicy.canShowAppOpen(
            nowMs = now,
            lastShownMs = lastShownMs,
            adLoadedAtMs = adLoadedAtMs,
            audioActive = audioActive,
            consentFormVisible = consentFormVisible,
            fullScreenAdVisible = fullScreenAdVisible,
        )
    }
}
