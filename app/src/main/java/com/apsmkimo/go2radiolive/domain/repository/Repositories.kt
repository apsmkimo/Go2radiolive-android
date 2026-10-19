package com.apsmkimo.go2radiolive.domain.repository

import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    suspend fun getLocalStations(limit: Int = 100): List<RadioStation>
    suspend fun getCountries(): List<CountryOption>
    suspend fun getStationsByCountry(countryCode: String, limit: Int = 100): List<RadioStation>
    suspend fun getCategories(): List<StationCategory>
    suspend fun getStationsByTag(tag: String, limit: Int = 100): List<RadioStation>
    suspend fun resolveStreamUrl(stationUuid: String): String?
}

interface PodcastRepository {
    suspend fun topShows(region: String?): List<PodcastShow>
    suspend fun episodes(show: PodcastShow, region: String?): List<RadioStation>
}

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<RadioStation>>
    suspend fun toggleFavorite(station: RadioStation)
    suspend fun isFavorite(stationUuid: String): Boolean
}
