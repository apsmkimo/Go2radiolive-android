package com.apsmkimo.go2radiolive.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.domain.station.PlayFailureRecord
import com.apsmkimo.go2radiolive.domain.station.StationFailurePolicy
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.playFailureDataStore by preferencesDataStore(
    name = "go2radio_play_failures",
)

/**
 * Consecutive playback failures, stored only on this device.
 * The map is never uploaded and is not an analytics event.
 */
class StationFailureStore(
    context: Context,
    private val gson: Gson,
) {
    private val dataStore = context.applicationContext.playFailureDataStore

    val records: Flow<Map<String, PlayFailureRecord>> = dataStore.data.map { prefs ->
        decode(prefs[KEY_FAILURES])
    }

    suspend fun recordSuccess(stationUuid: String, nowMs: Long) {
        if (stationUuid.isBlank()) return
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_FAILURES])
            val next = StationFailurePolicy.succeeded(current[stationUuid], nowMs)
            prefs[KEY_FAILURES] = gson.toJson(prune(current + (stationUuid to next)))
        }
    }

    suspend fun recordFailure(stationUuid: String, nowMs: Long) {
        if (stationUuid.isBlank()) return
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_FAILURES])
            val next = StationFailurePolicy.failed(current[stationUuid], nowMs)
            prefs[KEY_FAILURES] = gson.toJson(prune(current + (stationUuid to next)))
        }
    }

    private fun decode(json: String?): Map<String, PlayFailureRecord> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val type = object : TypeToken<Map<String, PlayFailureRecord>>() {}.type
            gson.fromJson<Map<String, PlayFailureRecord>>(json, type).orEmpty()
        }.getOrDefault(emptyMap())
    }

    private fun prune(records: Map<String, PlayFailureRecord>): Map<String, PlayFailureRecord> {
        if (records.size <= MAX_RECORDS) return records
        return records.entries
            .sortedByDescending { it.value.lastFailureAtMs }
            .take(MAX_RECORDS)
            .associate { it.key to it.value }
    }

    companion object {
        private val KEY_FAILURES = stringPreferencesKey("failures_json")
        private const val MAX_RECORDS = 400
    }
}
