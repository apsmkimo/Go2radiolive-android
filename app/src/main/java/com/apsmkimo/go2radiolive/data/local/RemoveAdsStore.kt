package com.apsmkimo.go2radiolive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.removeAdsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_remove_ads",
)

/**
 * Cached remove-ads entitlement. Read on cold start before Play Billing
 * responds so ads stay off offline, then overwritten by a successful
 * `queryPurchasesAsync` result (including a refund that drops the purchase).
 */
class RemoveAdsStore(context: Context) {
    private val dataStore = context.applicationContext.removeAdsDataStore

    data class Snapshot(
        val entitled: Boolean,
        val debugOverride: Boolean,
    )

    suspend fun read(): Snapshot {
        val prefs = dataStore.data.first()
        return Snapshot(
            entitled = prefs[KEY_ENTITLED] == true,
            debugOverride = prefs[KEY_DEBUG_OVERRIDE] == true,
        )
    }

    fun readBlocking(): Snapshot = runBlocking {
        runCatching { read() }.getOrDefault(Snapshot(entitled = false, debugOverride = false))
    }

    suspend fun setEntitled(entitled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_ENTITLED] = entitled }
    }

    suspend fun setDebugOverride(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DEBUG_OVERRIDE] = enabled }
    }

    companion object {
        private val KEY_ENTITLED = booleanPreferencesKey("entitled")
        private val KEY_DEBUG_OVERRIDE = booleanPreferencesKey("debug_override")
    }
}
