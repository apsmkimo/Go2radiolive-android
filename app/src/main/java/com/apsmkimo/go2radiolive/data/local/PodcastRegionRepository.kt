package com.apsmkimo.go2radiolive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.data.remote.AppleStorefronts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private val Context.podcastRegionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_podcast_region",
)

/**
 * Podcast chart storefront chosen by the user.
 * An empty selection means automatic (the same device region as the Local tab).
 */
class PodcastRegionRepository(context: Context) {
    private val dataStore = context.applicationContext.podcastRegionDataStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loaded = CompletableDeferred<Unit>()
    private val _selection = MutableStateFlow("")
    val selection: StateFlow<String> = _selection.asStateFlow()

    init {
        scope.launch {
            runCatching {
                dataStore.data.collect { prefs ->
                    _selection.value = normalize(prefs[KEY].orEmpty())
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

    suspend fun setSelection(code: String) {
        val normalized = normalize(code)
        _selection.value = normalized
        dataStore.edit { prefs ->
            if (normalized.isEmpty()) {
                prefs.remove(KEY)
            } else {
                prefs[KEY] = normalized
            }
        }
    }

    private fun normalize(raw: String): String {
        val code = raw.trim().lowercase()
        if (code.isEmpty()) return ""
        if (!AppleStorefronts.supports(code)) return ""
        return code
    }

    private companion object {
        val KEY = stringPreferencesKey("storefront")
    }
}
