package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.mapper.toDomain
import com.apsmkimo.go2radiolive.data.mapper.toDomainStations
import com.apsmkimo.go2radiolive.data.remote.RadioBrowserApi
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.repository.StationRepository

class StationRepositoryImpl(
    private val api: RadioBrowserApi,
) : StationRepository {

    override suspend fun getLocalStations(limit: Int): List<RadioStation> {
        return api.getStationsByCountry(countryCode = LOCAL_COUNTRY_CODE, limit = limit)
            .toDomainStations()
    }

    override suspend fun getWorldStations(limit: Int): List<RadioStation> {
        return api.searchStations(limit = limit, order = "clickcount")
            .toDomainStations()
    }

    override suspend fun getReservedStations(limit: Int): List<RadioStation> {
        return api.searchStations(limit = limit, order = "votes")
            .toDomainStations()
    }

    override suspend fun getCategories(): List<StationCategory> {
        return api.getTags()
            .mapNotNull { it.toDomain() }
            .filter { it.stationCount >= MIN_CATEGORY_STATIONS }
            .sortedByDescending { it.stationCount }
            .take(MAX_CATEGORIES)
    }

    override suspend fun getStationsByTag(tag: String, limit: Int): List<RadioStation> {
        return api.getStationsByTag(tag = tag, limit = limit).toDomainStations()
    }

    override suspend fun resolveStreamUrl(stationUuid: String): String? {
        val response = api.resolveStationUrl(stationUuid)
        return if (response.ok == true) response.url?.trim()?.takeIf { it.isNotEmpty() } else null
    }

    companion object {
        const val LOCAL_COUNTRY_CODE = "TW"
        private const val MIN_CATEGORY_STATIONS = 20
        private const val MAX_CATEGORIES = 60
    }
}
