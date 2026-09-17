package com.apsmkimo.go2radiolive.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoriteDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_favorites",
)

class FavoriteLocalDataSource(
    context: Context,
    private val gson: Gson,
) {
    private val dataStore = context.applicationContext.favoriteDataStore

    val favorites: Flow<List<RadioStation>> = dataStore.data.map { prefs ->
        decode(prefs[KEY_FAVORITES_JSON])
    }

    suspend fun save(stations: List<RadioStation>) {
        dataStore.edit { prefs ->
            prefs[KEY_FAVORITES_JSON] = gson.toJson(stations)
        }
    }

    private fun decode(json: String?): List<RadioStation> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<RadioStation>>() {}.type
            gson.fromJson<List<RadioStation>>(json, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    companion object {
        private val KEY_FAVORITES_JSON = stringPreferencesKey("favorites_json")
    }
}
