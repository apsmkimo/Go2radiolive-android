package com.apsmkimo.go2radiolive.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAdFeedTest {
    @Test
    fun nativeAdsFollowEveryEighthRow() {
        assertEquals(8, NATIVE_AD_INTERVAL)
        val inserts = (0 until 24).filter { nativeAdInsertAfterIndex(it) }
        assertEquals(listOf(7, 15, 23), inserts)
    }

    @Test
    fun shortListsHaveNoNativeAd() {
        assertFalse(nativeAdInsertAfterIndex(0))
        assertFalse(nativeAdInsertAfterIndex(6))
        assertTrue(nativeAdInsertAfterIndex(7))
        assertFalse(nativeAdInsertAfterIndex(-1))
    }
}
