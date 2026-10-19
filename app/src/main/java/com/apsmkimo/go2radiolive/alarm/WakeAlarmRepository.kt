package com.apsmkimo.go2radiolive.alarm

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.wakeAlarmDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_wake_alarm",
)

class WakeAlarmRepository(
    context: Context,
    private val gson: Gson,
) {
    private val dataStore = context.applicationContext.wakeAlarmDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loaded = CompletableDeferred<Unit>()
    private val _alarm = MutableStateFlow(WakeAlarm())
    val alarm: StateFlow<WakeAlarm> = _alarm.asStateFlow()
    private val _lastStation = MutableStateFlow<RadioStation?>(null)
    val lastStation: StateFlow<RadioStation?> = _lastStation.asStateFlow()

    init {
        scope.launch {
            runCatching {
                dataStore.data.collect { prefs ->
                    _alarm.value = decode(prefs[ALARM], WakeAlarm())
                    _lastStation.value = decode(prefs[LAST], null as RadioStation?)
                    if (!loaded.isCompleted) loaded.complete(Unit)
                }
            }.onFailure {
                if (!loaded.isCompleted) loaded.complete(Unit)
            }
        }
    }

    suspend fun awaitLoaded() {
        loaded.await()
    }

    fun remember(station: RadioStation) {
        if (station.stationUuid.isBlank() || station.streamUrl.isBlank()) return
        _lastStation.value = station
        scope.launch {
            dataStore.edit { prefs -> prefs[LAST] = gson.toJson(station) }
        }
    }

    suspend fun save(alarm: WakeAlarm) {
        val normalized = alarm.copy(
            hour = alarm.hour.coerceIn(0, 23),
            minute = alarm.minute.coerceIn(0, 59),
            repeatDays = alarm.repeatDays.distinct().filter { it in 1..7 }.sorted(),
        )
        _alarm.value = normalized
        dataStore.edit { prefs -> prefs[ALARM] = gson.toJson(normalized) }
    }

    private inline fun <reified T> decode(json: String?, fallback: T): T {
        if (json.isNullOrBlank()) return fallback
        return runCatching { gson.fromJson(json, T::class.java) }.getOrDefault(fallback)
    }

    private companion object {
        val ALARM = stringPreferencesKey("alarm")
        val LAST = stringPreferencesKey("last_station")
    }
}
