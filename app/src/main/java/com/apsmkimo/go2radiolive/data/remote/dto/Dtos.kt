package com.apsmkimo.go2radiolive.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StationDto(
    @SerializedName("stationuuid") val stationUuid: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("url_resolved") val urlResolved: String? = null,
    @SerializedName("homepage") val homepage: String? = null,
    @SerializedName("favicon") val favicon: String? = null,
    @SerializedName("tags") val tags: String? = null,
    @SerializedName("country") val country: String? = null,
    @SerializedName("countrycode") val countryCode: String? = null,
    // SMCPKG_SUPPORT>>>Cursor106
    @SerializedName("language") val language: String? = null,
    @SerializedName("bitrate") val bitrate: Int? = null,
    @SerializedName("votes") val votes: Int? = null,
    @SerializedName("clickcount") val clickCount: Int? = null,
    // SMCPKG_SUPPORT<<<Cursor106
)

data class TagDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("stationcount") val stationCount: Int? = null,
)

data class CountryDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("iso_3166_1") val isoCode: String? = null,
    @SerializedName("stationcount") val stationCount: Int? = null,
)

// SMCPKG_SUPPORT>>>Cursor106
data class LanguageDto(
    @SerializedName("name") val name: String? = null,
    @SerializedName("stationcount") val stationCount: Int? = null,
)
// SMCPKG_SUPPORT<<<Cursor106

data class ClickResponseDto(
    @SerializedName("ok") val ok: Boolean? = null,
    @SerializedName("url") val url: String? = null,
    @SerializedName("message") val message: String? = null,
)
