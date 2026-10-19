package com.apsmkimo.go2radiolive.data.repository

import com.apsmkimo.go2radiolive.data.mapper.toDomain
import com.apsmkimo.go2radiolive.data.mapper.toDomainStations
import com.apsmkimo.go2radiolive.data.remote.cacheBustToken
import com.apsmkimo.go2radiolive.data.remote.cacheControlHeader
import com.apsmkimo.go2radiolive.data.remote.localStationsCountry
import com.apsmkimo.go2radiolive.data.remote.normalizeRegion
import com.apsmkimo.go2radiolive.data.remote.RadioBrowserApi
import com.apsmkimo.go2radiolive.data.remote.dto.CountryDto
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.repository.StationRepository

class StationRepositoryImpl(
    private val api: RadioBrowserApi,
) : StationRepository {

    // SMCPKG_SUPPORT>>>Cursor074
    // override suspend fun getLocalStations(limit: Int): List<RadioStation> {
    //     return getStationsByCountry(LOCAL_COUNTRY_CODE, limit)
    // }
    override suspend fun getLocalStations(
        countryHint: String?,
        limit: Int,
        bypassCache: Boolean,
    ): List<RadioStation> {
        val primary = normalizeRegion(countryHint) ?: "US"
        val stations = getStationsByCountry(primary, limit, bypassCache)
        val chosen = localStationsCountry(primary, stations.size)
        return if (chosen == primary) stations else getStationsByCountry(chosen, limit, bypassCache)
    }
    // SMCPKG_SUPPORT<<<Cursor074

    override suspend fun getCountries(bypassCache: Boolean): List<CountryOption> {
        return api.getCountries(
            cacheControl = cacheControlHeader(bypassCache),
            cacheBust = cacheBustToken(bypassCache),
        )
            .mapNotNull { country: CountryDto -> country.toDomain() }
            .sortedByDescending { it.stationCount }
    }

    override suspend fun getStationsByCountry(
        countryCode: String,
        limit: Int,
        bypassCache: Boolean,
    ): List<RadioStation> {
        val code = countryCode.trim().uppercase()
        if (code.length != 2) return emptyList()
        return api.getStationsByCountry(
            countryCode = code,
            limit = limit,
            cacheControl = cacheControlHeader(bypassCache),
            cacheBust = cacheBustToken(bypassCache),
        ).toDomainStations()
    }

    // SMCPKG_SUPPORT>>>Cursor067
    // override suspend fun getWorldStations(limit: Int): List<RadioStation> {
    //     return api.searchStations(limit = limit, order = "clickcount").toDomainStations()
    // }
    //
    // override suspend fun getReservedStations(limit: Int): List<RadioStation> {
    //     return api.searchStations(limit = limit, order = "votes").toDomainStations()
    // }
    // SMCPKG_SUPPORT<<<Cursor067

    override suspend fun getCategories(bypassCache: Boolean): List<StationCategory> {
        return api.getTags(
            cacheControl = cacheControlHeader(bypassCache),
            cacheBust = cacheBustToken(bypassCache),
        )
            .mapNotNull { it.toDomain() }
            .filter { it.stationCount >= MIN_CATEGORY_STATIONS }
            .sortedByDescending { it.stationCount }
            .take(MAX_CATEGORIES)
    }

    override suspend fun getStationsByTag(tag: String, limit: Int, bypassCache: Boolean): List<RadioStation> {
        return api.getStationsByTag(
            tag = tag,
            limit = limit,
            cacheControl = cacheControlHeader(bypassCache),
            cacheBust = cacheBustToken(bypassCache),
        ).toDomainStations()
    }

    override suspend fun resolveStreamUrl(stationUuid: String): String? {
        val response = api.resolveStationUrl(stationUuid)
        return if (response.ok == true) response.url?.trim()?.takeIf { it.isNotEmpty() } else null
    }

    companion object {
        // const val LOCAL_COUNTRY_CODE = "TW"
        private const val MIN_CATEGORY_STATIONS = 20
        private const val MAX_CATEGORIES = 60
    }
}
