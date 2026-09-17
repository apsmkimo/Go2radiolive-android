package com.apsmkimo.go2radiolive.domain.model

data class RadioStation(
    val stationUuid: String,
    val name: String,
    val streamUrl: String,
    val homepage: String = "",
    val faviconUrl: String = "",
    val tags: String = "",
    val country: String = "",
    val countryCode: String = "",
)

data class StationCategory(
    val name: String,
    val stationCount: Int,
)

data class PlaybackState(
    val station: RadioStation? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val errorMessage: String? = null,
)

enum class BrowseTab {
    LOCAL,
    WORLD,
    RESERVED,
    CATEGORIES,
    FAVORITES,
}
