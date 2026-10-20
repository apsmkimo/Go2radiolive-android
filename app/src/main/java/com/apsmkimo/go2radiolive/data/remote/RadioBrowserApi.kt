package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.data.remote.dto.ClickResponseDto
import com.apsmkimo.go2radiolive.data.remote.dto.CountryDto
import com.apsmkimo.go2radiolive.data.remote.dto.LanguageDto
import com.apsmkimo.go2radiolive.data.remote.dto.StationDto
import com.apsmkimo.go2radiolive.data.remote.dto.TagDto
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    /**
     * Stations for one country, most-clicked first.
     * GET /json/stations/bycountrycodeexact/{cc}?hidebroken=true&order=clickcount&reverse=true&limit=500
     *
     * limit 100 omitted ICRT: on 2026-10-19 the healthy TW list was 193 stations and
     * "Radio ICRT" / "ICRT" ranked 102 and 159 (both lastcheckok=1).
     */
    @GET("json/stations/bycountrycodeexact/{countryCode}")
    suspend fun getStationsByCountry(
        @Path("countryCode") countryCode: String,
        @Query("limit") limit: Int = 500,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<StationDto>

    @GET("json/countries")
    suspend fun getCountries(
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<CountryDto>

    /**
     * Worldwide search kept for callers that still want a flat list.
     */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("limit") limit: Int = 100,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        // SMCPKG_SUPPORT>>>Cursor106
        @Query("countrycode") countryCode: String? = null,
        @Query("tag") tag: String? = null,
        @Query("tagExact") tagExact: Boolean? = null,
        @Query("language") language: String? = null,
        @Query("languageExact") languageExact: Boolean? = null,
        @Query("bitrateMin") bitrateMin: Int? = null,
        // SMCPKG_SUPPORT<<<Cursor106
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<StationDto>

    @GET("json/stations/bytagexact/{tag}")
    suspend fun getStationsByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 100,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<StationDto>

    @GET("json/tags")
    suspend fun getTags(
        // SMCPKG_SUPPORT>>>Cursor106
        @Query("order") order: String? = null,
        @Query("reverse") reverse: Boolean? = null,
        @Query("limit") limit: Int? = null,
        // SMCPKG_SUPPORT<<<Cursor106
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<TagDto>

    // SMCPKG_SUPPORT>>>Cursor106
    @GET("json/languages")
    suspend fun getLanguages(
        @Query("order") order: String? = null,
        @Query("reverse") reverse: Boolean? = null,
        @Query("limit") limit: Int? = null,
        @Header("Cache-Control") cacheControl: String? = null,
        @Query("cb") cacheBust: Long? = null,
    ): List<LanguageDto>
    // SMCPKG_SUPPORT<<<Cursor106

    @GET("json/url/{stationUuid}")
    suspend fun resolveStationUrl(
        @Path("stationUuid") stationUuid: String,
    ): ClickResponseDto
}
