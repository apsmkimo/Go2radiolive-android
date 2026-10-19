package com.apsmkimo.go2radiolive.presentation.player

import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.gson.Gson

internal const val EXTRA_STATION_JSON = "extra_station_json"

internal fun stationMediaItem(
    context: Context,
    gson: Gson,
    station: RadioStation,
    streamUrl: String,
): MediaItem {
    val extras = Bundle().apply {
        putString(EXTRA_STATION_JSON, gson.toJson(station))
    }
    val artwork = station.faviconUrl.takeIf { it.isNotBlank() }?.toUri()
    return MediaItem.Builder()
        .setMediaId(station.stationUuid)
        .setUri(streamUrl)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(station.name)
                .setArtist(station.country.ifBlank { context.getString(R.string.app_name) })
                .setArtworkUri(artwork)
                .setIsPlayable(true)
                .setExtras(extras)
                .build(),
        )
        .build()
}
