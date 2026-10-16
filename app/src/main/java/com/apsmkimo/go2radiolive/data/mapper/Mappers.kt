package com.apsmkimo.go2radiolive.data.mapper

import com.apsmkimo.go2radiolive.data.remote.dto.StationDto
import com.apsmkimo.go2radiolive.data.remote.dto.TagDto
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory

fun StationDto.toDomain(): RadioStation? {
    val uuid = stationUuid?.trim().orEmpty()
    val displayName = name?.trim().orEmpty()
    val stream = urlResolved?.trim().takeUnless { it.isNullOrEmpty() }
        ?: url?.trim().orEmpty()
    if (uuid.isEmpty() || displayName.isEmpty() || stream.isEmpty()) return null
    return RadioStation(
        stationUuid = uuid,
        name = displayName,
        streamUrl = stream,
        homepage = homepage.orEmpty(),
        faviconUrl = favicon.orEmpty(),
        tags = tags.orEmpty(),
        country = country.orEmpty(),
        countryCode = countryCode.orEmpty(),
    )
}

fun TagDto.toDomain(): StationCategory? {
    val tagName = name?.trim().orEmpty()
    if (tagName.isEmpty()) return null
    return StationCategory(
        name = tagName,
        stationCount = stationCount ?: 0,
    )
}

fun List<StationDto>.toDomainStations(): List<RadioStation> =
    mapNotNull { it.toDomain() }
        .distinctBy { it.stationUuid }
