package com.apsmkimo.go2radiolive.data.remote

import com.apsmkimo.go2radiolive.data.remote.dto.ClickResponseDto
import com.apsmkimo.go2radiolive.data.remote.dto.StationDto
import com.apsmkimo.go2radiolive.data.remote.dto.TagDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    /**
     * Local (Taiwan) stations.
     * GET /json/stations/bycountrycodeexact/TW?limit=100
     */
    @GET("json/stations/bycountrycodeexact/{countryCode}")
    suspend fun getStationsByCountry(
        @Path("countryCode") countryCode: String,
        @Query("limit") limit: Int = 100,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
    ): List<StationDto>

    /**
     * World / reserved search.
     * World: order=clickcount (most clicked worldwide).
     * Reserved: order=votes (highest voted featured placeholder).
     */
    @GET("json/stations/search")
    suspend fun searchStations(
        @Query("limit") limit: Int = 100,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
    ): List<StationDto>

    @GET("json/stations/bytagexact/{tag}")
    suspend fun getStationsByTag(
        @Path("tag") tag: String,
        @Query("limit") limit: Int = 100,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
    ): List<StationDto>

    @GET("json/tags")
    suspend fun getTags(): List<TagDto>

    @GET("json/url/{stationUuid}")
    suspend fun resolveStationUrl(
        @Path("stationUuid") stationUuid: String,
    ): ClickResponseDto
}
