package com.apsmkimo.go2radiolive.data.local

// SMCPKG_SUPPORT>>>Cursor123
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoriteShowDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "go2radio_favorite_shows",
)

/** Favorite podcast shows, kept apart from the per-episode / station favorites. */
class FavoriteShowLocalDataSource(
    context: Context,
    private val gson: Gson,
) {
    private val dataStore = context.applicationContext.favoriteShowDataStore

    val shows: Flow<List<PodcastShow>> = dataStore.data.map { prefs ->
        decode(prefs[KEY_SHOWS_JSON])
    }

    /** Adds the show at the top, or removes it when it is already saved. */
    suspend fun toggle(show: PodcastShow) {
        dataStore.edit { prefs ->
            val current = decode(prefs[KEY_SHOWS_JSON])
            prefs[KEY_SHOWS_JSON] = gson.toJson(toggledFavoriteShows(current, show))
        }
    }

    private fun decode(json: String?): List<PodcastShow> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PodcastShow>>() {}.type
            gson.fromJson<List<PodcastShow?>>(json, type).orEmpty()
                .filterNotNull()
                .filter { !it.id.isNullOrBlank() }
                .map { it.normalized() }
        }.getOrDefault(emptyList())
    }

    companion object {
        private val KEY_SHOWS_JSON = stringPreferencesKey("favorite_shows_json")
    }
}

/** Gson can leave non-null Kotlin fields null when an older or partial row is read. */
@Suppress("SENSELESS_COMPARISON", "USELESS_ELVIS")
private fun PodcastShow.normalized(): PodcastShow = PodcastShow(
    id = id,
    name = name ?: "",
    author = author ?: "",
    artworkUrl = artworkUrl ?: "",
    feedUrl = feedUrl?.takeIf { it.isNotBlank() },
)

internal fun toggledFavoriteShows(current: List<PodcastShow>, show: PodcastShow): List<PodcastShow> {
    return if (current.any { it.id == show.id }) {
        current.filterNot { it.id == show.id }
    } else {
        listOf(show.copy(feedUrl = show.feedUrl?.takeIf { it.isNotBlank() })) + current
    }
}
// SMCPKG_SUPPORT<<<Cursor123
