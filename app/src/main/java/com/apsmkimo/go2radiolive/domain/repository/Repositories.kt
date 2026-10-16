package com.apsmkimo.go2radiolive.domain.repository

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import kotlinx.coroutines.flow.Flow

interface StationRepository {
    suspend fun getLocalStations(limit: Int = 100): List<RadioStation>
    suspend fun getWorldStations(limit: Int = 100): List<RadioStation>
    suspend fun getReservedStations(limit: Int = 100): List<RadioStation>
    suspend fun getCategories(): List<StationCategory>
    suspend fun getStationsByTag(tag: String, limit: Int = 100): List<RadioStation>
    suspend fun resolveStreamUrl(stationUuid: String): String?
}

interface FavoriteRepository {
    fun observeFavorites(): Flow<List<RadioStation>>
    suspend fun toggleFavorite(station: RadioStation)
    suspend fun isFavorite(stationUuid: String): Boolean
}
