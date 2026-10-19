package com.apsmkimo.go2radiolive.ads

/** One native ad after this many content rows. */
internal const val NATIVE_AD_INTERVAL = 8

/** True when a native ad slot follows the content row at [index] (0-based). */
internal fun nativeAdInsertAfterIndex(index: Int, interval: Int = NATIVE_AD_INTERVAL): Boolean {
    if (interval <= 0 || index < 0) return false
    return (index + 1) % interval == 0
}
