package com.apsmkimo.go2radiolive.domain.repository

import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import kotlinx.coroutines.flow.Flow

/**
 * Healthy Taiwan stations measured 2026-10-19 on de1 (hidebroken=true, clickcount desc): 193.
 * "Radio ICRT" was rank 102 and "ICRT" rank 159, so a limit of 100 dropped both.
 * 500 keeps that catalog, with room to grow. Larger countries stay at the 500 most-clicked.
 */
const val COUNTRY_STATION_LIMIT = 500

interface StationRepository {
    suspend fun getLocalStations(
        countryHint: String? = null,
        limit: Int = COUNTRY_STATION_LIMIT,
        bypassCache: Boolean = false,
    ): List<RadioStation>
    suspend fun getCountries(bypassCache: Boolean = false): List<CountryOption>
    suspend fun getStationsByCountry(
        countryCode: String,
        limit: Int = COUNTRY_STATION_LIMIT,
        bypassCache: Boolean = false,
    ): List<RadioStation>
    suspend fun getCategories(bypassCache: Boolean = false): List<StationCategory>
    suspend fun getStationsByTag(
        tag: String,
        limit: Int = 100,
        bypassCache: Boolean = false,
    ): List<RadioStation>
    suspend fun resolveStreamUrl(stationUuid: String): String?
}

interface PodcastRepository {
    // SMCPKG_SUPPORT>>>Cursor103
    // suspend fun topShows(region: String?, bypassCache: Boolean = false): PodcastChart
    /** Emits the top chart first, then the same list with genre shows merged in. */
    fun topShows(region: String?, bypassCache: Boolean = false): Flow<PodcastChart>
    // SMCPKG_SUPPORT<<<Cursor103
    suspend fun episodes(show: PodcastShow, region: String?, bypassCache: Boolean = false): List<RadioStation>
}

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<RadioStation>>
    suspend fun toggleFavorite(station: RadioStation)
    suspend fun isFavorite(stationUuid: String): Boolean
}
