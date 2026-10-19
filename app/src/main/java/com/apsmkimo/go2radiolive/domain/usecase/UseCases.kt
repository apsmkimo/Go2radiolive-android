package com.apsmkimo.go2radiolive.domain.usecase

import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.repository.FavoriteRepository
import com.apsmkimo.go2radiolive.domain.repository.PodcastRepository
import com.apsmkimo.go2radiolive.domain.repository.StationRepository


class GetStationsUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(
        tab: BrowseTab,
        categoryTag: String? = null,
        countryCode: String? = null,
        bypassCache: Boolean = false,
    ): List<RadioStation> {
        return when (tab) {
            BrowseTab.LOCAL -> stationRepository.getLocalStations(countryHint = countryCode, bypassCache = bypassCache)
            BrowseTab.WORLD -> {
                val code = countryCode?.takeIf { it.isNotBlank() } ?: return emptyList()
                stationRepository.getStationsByCountry(code, bypassCache = bypassCache)
            }
            // SMCPKG_SUPPORT>>>Cursor068
            // BrowseTab.RESERVED -> stationRepository.getReservedStations()
            BrowseTab.PODCAST -> emptyList()
            // SMCPKG_SUPPORT<<<Cursor068
            BrowseTab.CATEGORIES -> {
                val tag = categoryTag?.takeIf { it.isNotBlank() } ?: return emptyList()
                stationRepository.getStationsByTag(tag, bypassCache = bypassCache)
            }
            BrowseTab.FAVORITES -> emptyList()
        }
    }
}

class GetCountriesUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(bypassCache: Boolean = false): List<CountryOption> =
        stationRepository.getCountries(bypassCache)
}

class GetPodcastShowsUseCase(
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(region: String?, bypassCache: Boolean = false): PodcastChart =
        podcastRepository.topShows(region, bypassCache)
}

class GetPodcastEpisodesUseCase(
    private val podcastRepository: PodcastRepository,
) {
    suspend operator fun invoke(
        show: PodcastShow,
        region: String?,
        bypassCache: Boolean = false,
    ): List<RadioStation> {
        return podcastRepository.episodes(show, region, bypassCache)
    }
}

class GetCategoriesUseCase(
    private val stationRepository: StationRepository,
) {
    suspend operator fun invoke(bypassCache: Boolean = false): List<StationCategory> =
        stationRepository.getCategories(bypassCache)
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
