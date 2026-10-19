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
    /** Podcast enclosures already point at the media file and skip Radio-Browser. */
    val directStream: Boolean = false,
)

data class StationCategory(
    val name: String,
    val stationCount: Int,
)

data class CountryOption(
    val code: String,
    val name: String,
    val stationCount: Int,
)

data class PodcastShow(
    val id: String,
    val name: String,
    val author: String,
    val artworkUrl: String = "",
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
    // SMCPKG_SUPPORT>>>Cursor066
    // RESERVED,
    PODCAST,
    // SMCPKG_SUPPORT<<<Cursor066
    CATEGORIES,
    FAVORITES,
}
