package com.apsmkimo.go2radiolive.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Whether banner, native, and app-open ads must stay off.
 * Seeded from DataStore before the first activity, then updated when billing
 * confirms a purchase or a refund.
 */
object AdsEntitlement {
    private val _removed = MutableStateFlow(false)
    val removed: StateFlow<Boolean> = _removed.asStateFlow()

    fun publish(removed: Boolean) {
        _removed.value = removed
    }
}
