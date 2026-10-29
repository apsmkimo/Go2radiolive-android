package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.local.FavoriteLocalDataSource
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.repository.FavoriteRepository
import com.apsmkimo.go2radiolive.domain.station.identityIds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FavoriteRepositoryImpl(
    private val localDataSource: FavoriteLocalDataSource,
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<RadioStation>> = localDataSource.favorites

    override suspend fun toggleFavorite(station: RadioStation) {
        val current = localDataSource.favorites.first()
        // SMCPKG_SUPPORT>>>Cursor119
        // val updated = if (current.any { it.stationUuid == station.stationUuid }) {
        //     current.filterNot { it.stationUuid == station.stationUuid }
        // } else {
        //     listOf(station) + current
        // }
        val ids = station.identityIds()
        val updated = if (current.any { saved -> saved.identityIds().any { it in ids } }) {
            current.filterNot { saved -> saved.identityIds().any { it in ids } }
        } else {
            listOf(station) + current
        }
        // SMCPKG_SUPPORT<<<Cursor119
        localDataSource.save(updated)
    }

    override suspend fun isFavorite(stationUuid: String): Boolean {
        return localDataSource.favorites.first().any { it.stationUuid == stationUuid }
    }
}
