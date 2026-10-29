package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation

fun RadioStation.identityIds(): Set<String> {
    val ids = LinkedHashSet<String>()
    if (stationUuid.isNotBlank()) ids.add(stationUuid)
    mergedUuids.orEmpty().forEach { id ->
        if (id.isNotBlank()) ids.add(id)
    }
    return ids
}

fun stationIsFavorite(station: RadioStation, favoriteIds: Set<String>): Boolean {
    if (station.stationUuid in favoriteIds) return true
    return station.mergedUuids.orEmpty().any { it in favoriteIds }
}

enum class StationIconMotion {
    NONE,
    SPINNER,
    BARS,
    BARS_PAUSED,
}

fun stationIconMotion(stationId: String, playback: PlaybackState): StationIconMotion {
    val current = playback.station ?: return StationIconMotion.NONE
    if (current.stationUuid != stationId) return StationIconMotion.NONE
    if (playback.cannotConnect) return StationIconMotion.NONE
    if (playback.isBuffering) return StationIconMotion.SPINNER
    if (playback.isPlaying) return StationIconMotion.BARS
    return StationIconMotion.BARS_PAUSED
}
