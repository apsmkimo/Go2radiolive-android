package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.local.FavoriteLocalDataSource
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FavoriteRepositoryImpl(
    private val localDataSource: FavoriteLocalDataSource,
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<RadioStation>> = localDataSource.favorites

    override suspend fun toggleFavorite(station: RadioStation) {
        val current = localDataSource.favorites.first()
        val updated = if (current.any { it.stationUuid == station.stationUuid }) {
            current.filterNot { it.stationUuid == station.stationUuid }
        } else {
            listOf(station) + current
        }
        localDataSource.save(updated)
    }

    override suspend fun isFavorite(stationUuid: String): Boolean {
        return localDataSource.favorites.first().any { it.stationUuid == stationUuid }
    }
}
