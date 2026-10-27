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
    // SMCPKG_SUPPORT>>>Cursor106
    val language: String = "",
    val bitrate: Int = 0,
    val votes: Int = 0,
    val clickCount: Int = 0,
    // SMCPKG_SUPPORT<<<Cursor106
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
    // SMCPKG_SUPPORT>>>Cursor115
    /** iTunes Search feed. Chart rows leave this empty and look the feed up by id. */
    val feedUrl: String? = null,
    // SMCPKG_SUPPORT<<<Cursor115
)

/** Chart page plus whether the device region was replaced by the US chart. */
data class PodcastChart(
    val shows: List<PodcastShow>,
    val usedUsFallback: Boolean = false,
    // SMCPKG_SUPPORT>>>Cursor103
    /** False while genre feeds are still being merged after the top chart. */
    val complete: Boolean = true,
    // SMCPKG_SUPPORT<<<Cursor103
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
