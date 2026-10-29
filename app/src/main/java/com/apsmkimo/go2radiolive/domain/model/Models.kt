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
    // SMCPKG_SUPPORT>>>Cursor117
    /** Plain-text podcast show notes. Null on radio stations and older saved rows. */
    val summary: String? = null,
    // SMCPKG_SUPPORT<<<Cursor117
    // SMCPKG_SUPPORT>>>Cursor119
    /** Other stream addresses: the unused url/url_resolved pair and merged duplicates. */
    val alternateUrls: List<String>? = null,
    /** Uuids folded into this row. Older saved rows leave this null. */
    val mergedUuids: List<String>? = null,
    /** 1 = last check succeeded, 0 = failed, null = unknown on older saved rows. */
    val lastCheckOk: Int? = null,
    val lastCheckTimeMs: Long = 0,
    val codec: String? = null,
    val sslError: Boolean = false,
    val hls: Boolean = false,
    // SMCPKG_SUPPORT<<<Cursor119
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
    // SMCPKG_SUPPORT>>>Cursor119
    /** Every saved stream was tried and the station still did not connect. */
    val cannotConnect: Boolean = false,
    // SMCPKG_SUPPORT<<<Cursor119
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
