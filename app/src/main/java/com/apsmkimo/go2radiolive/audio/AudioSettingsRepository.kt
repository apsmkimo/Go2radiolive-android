package com.apsmkimo.go2radiolive.audio

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val Context.audioSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_audio_settings",
)

/**
 * Process-wide audio settings. The UI and [com.apsmkimo.go2radiolive.presentation.player.RadioPlaybackService]
 * share this object (same process). Values are persisted with DataStore and published on a
 * [StateFlow] so the DSP can read them on the audio thread without disk I/O.
 */
object AudioSettingsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startLock = Any()
    private val loaded = CountDownLatch(1)

    private val _settings = MutableStateFlow(AudioSettings())
    val settings: StateFlow<AudioSettings> = _settings.asStateFlow()

    @Volatile
    private var started = false

    @Volatile
    private var userEdited = false

    private lateinit var dataStore: DataStore<Preferences>
    private var persistJob: Job? = null

    fun start(context: Context) {
        val shouldLoad = synchronized(startLock) {
            if (started) {
                false
            } else {
                started = true
                dataStore = context.applicationContext.audioSettingsDataStore
                true
            }
        }
        if (shouldLoad) {
            scope.launch {
                try {
                    val decoded = runCatching { dataStore.data.first().toAudioSettings() }
                        .getOrDefault(AudioSettings())
                    synchronized(startLock) {
                        if (!userEdited) {
                            _settings.value = decoded
                        }
                    }
                } finally {
                    loaded.countDown()
                }
            }
        }
        // Bound the wait so a slow disk never freezes startup. Later buffers pick up the flow.
        loaded.await(500, TimeUnit.MILLISECONDS)
    }

    fun update(transform: (AudioSettings) -> AudioSettings) {
        val next = synchronized(startLock) {
            userEdited = true
            val updated = transform(_settings.value).sanitized()
            _settings.value = updated
            updated
        }
        schedulePersist(next)
    }

    fun setShowPanelButton(enabled: Boolean) = update { it.copy(showPanelButton = enabled) }

    fun setBassDb(db: Float) = update { it.copy(bassDb = db) }

    fun setLowDb(db: Float) = update { it.copy(lowDb = db) }

    fun setMidDb(db: Float) = update { it.copy(midDb = db) }

    fun setHighDb(db: Float) = update { it.copy(highDb = db) }

    fun setBalance(balance: Float) = update { it.copy(balance = balance) }

    fun setVoiceEnhancement(enabled: Boolean) = update { it.copy(voiceEnhancement = enabled) }

    // SMCPKG_SUPPORT>>>Cursor059
    // fun setVirtualSurround(enabled: Boolean) = update { it.copy(virtualSurround = enabled) }
    fun setSpatialMode(mode: SpatialAudioMode) = update { it.copy(spatialMode = mode) }

    fun setOrbitPeriodSec(seconds: Float) = update { it.copy(orbitPeriodSec = seconds) }

    fun setOrbitClockwise(clockwise: Boolean) = update { it.copy(orbitClockwise = clockwise) }

    fun setOrbitStrength(strength: Float) = update { it.copy(orbitStrength = strength) }
    // SMCPKG_SUPPORT<<<Cursor059

    fun resetTone() = update {
        it.copy(bassDb = 0f, lowDb = 0f, midDb = 0f, highDb = 0f, balance = 0f)
    }

    private fun schedulePersist(snapshot: AudioSettings) {
        if (!::dataStore.isInitialized) return
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(120)
            val latest = _settings.value
            // Prefer the newest in-memory value if another edit landed after this snapshot.
            val toWrite = if (latest == snapshot) snapshot else latest
            dataStore.edit { prefs -> prefs.writeAudioSettings(toWrite) }
        }
    }

    private fun Preferences.toAudioSettings(): AudioSettings = AudioSettings(
        showPanelButton = this[KEY_SHOW_PANEL] ?: false,
        bassDb = this[KEY_BASS] ?: 0f,
        lowDb = this[KEY_LOW] ?: 0f,
        midDb = this[KEY_MID] ?: 0f,
        highDb = this[KEY_HIGH] ?: 0f,
        balance = this[KEY_BALANCE] ?: 0f,
        voiceEnhancement = this[KEY_VOICE] ?: false,
        // SMCPKG_SUPPORT>>>Cursor060
        // virtualSurround = this[KEY_SURROUND] ?: false,
        spatialMode = SpatialAudioMode.fromStored(this[KEY_SPATIAL_MODE]),
        orbitPeriodSec = this[KEY_ORBIT_PERIOD] ?: 60f,
        orbitClockwise = this[KEY_ORBIT_CLOCKWISE] ?: true,
        orbitStrength = this[KEY_ORBIT_STRENGTH] ?: 0.6f,
        // SMCPKG_SUPPORT<<<Cursor060
    ).sanitized()

    private fun androidx.datastore.preferences.core.MutablePreferences.writeAudioSettings(
        settings: AudioSettings,
    ) {
        this[KEY_SHOW_PANEL] = settings.showPanelButton
        this[KEY_BASS] = settings.bassDb
        this[KEY_LOW] = settings.lowDb
        this[KEY_MID] = settings.midDb
        this[KEY_HIGH] = settings.highDb
        this[KEY_BALANCE] = settings.balance
        this[KEY_VOICE] = settings.voiceEnhancement
        // SMCPKG_SUPPORT>>>Cursor061
        // this[KEY_SURROUND] = settings.virtualSurround
        this[KEY_SPATIAL_MODE] = settings.spatialMode.name
        this[KEY_ORBIT_PERIOD] = settings.orbitPeriodSec
        this[KEY_ORBIT_CLOCKWISE] = settings.orbitClockwise
        this[KEY_ORBIT_STRENGTH] = settings.orbitStrength
        // SMCPKG_SUPPORT<<<Cursor061
    }

    private val KEY_SHOW_PANEL = booleanPreferencesKey("show_panel_button")
    private val KEY_BASS = floatPreferencesKey("bass_db")
    private val KEY_LOW = floatPreferencesKey("low_db")
    private val KEY_MID = floatPreferencesKey("mid_db")
    private val KEY_HIGH = floatPreferencesKey("high_db")
    private val KEY_BALANCE = floatPreferencesKey("balance")
    private val KEY_VOICE = booleanPreferencesKey("voice_enhancement")
    // SMCPKG_SUPPORT>>>Cursor062
    // private val KEY_SURROUND = booleanPreferencesKey("virtual_surround")
    private val KEY_SPATIAL_MODE = stringPreferencesKey("spatial_mode")
    private val KEY_ORBIT_PERIOD = floatPreferencesKey("orbit_period_sec")
    private val KEY_ORBIT_CLOCKWISE = booleanPreferencesKey("orbit_clockwise")
    private val KEY_ORBIT_STRENGTH = floatPreferencesKey("orbit_strength")
    // SMCPKG_SUPPORT<<<Cursor062
}
