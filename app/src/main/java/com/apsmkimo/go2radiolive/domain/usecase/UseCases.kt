package com.apsmkimo.go2radiolive.domain.usecase

import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.repository.FavoriteRepository
import com.apsmkimo.go2radiolive.domain.repository.StationRepository


class GetStationsUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(tab: BrowseTab, categoryTag: String? = null): List<RadioStation> {
        return when (tab) {
            BrowseTab.LOCAL -> stationRepository.getLocalStations()
            BrowseTab.WORLD -> stationRepository.getWorldStations()
            BrowseTab.RESERVED -> stationRepository.getReservedStations()
            BrowseTab.CATEGORIES -> {
                val tag = categoryTag?.takeIf { it.isNotBlank() } ?: return emptyList()
                stationRepository.getStationsByTag(tag)
            }
            BrowseTab.FAVORITES -> emptyList()
        }
    }
}

class GetCategoriesUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(): List<StationCategory> = stationRepository.getCategories()
}

class ObserveFavoritesUseCase(
    private val favoriteRepository: FavoriteRepository,
) {
    operator fun invoke() = favoriteRepository.observeFavorites()
}

class ToggleFavoriteUseCase(
    private val favoriteRepository: FavoriteRepository,
) {
    suspend operator fun invoke(station: RadioStation) = favoriteRepository.toggleFavorite(station)
}

class ResolveStreamUrlUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(station: RadioStation): String {
        return stationRepository.resolveStreamUrl(station.stationUuid)
            ?.takeIf { it.isNotBlank() }
            ?: station.streamUrl
    }
}
