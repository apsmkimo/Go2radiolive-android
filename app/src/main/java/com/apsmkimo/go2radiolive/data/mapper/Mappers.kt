package com.apsmkimo.go2radiolive.data.mapper

import com.apsmkimo.go2radiolive.data.remote.dto.CountryDto
import com.apsmkimo.go2radiolive.data.remote.dto.StationDto
import com.apsmkimo.go2radiolive.data.remote.dto.TagDto
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import java.util.Locale

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
        // SMCPKG_SUPPORT>>>Cursor106
        language = language.orEmpty(),
        bitrate = bitrate ?: 0,
        votes = votes ?: 0,
        clickCount = clickCount ?: 0,
        // SMCPKG_SUPPORT<<<Cursor106
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

fun CountryDto.toDomain(locale: Locale = Locale.getDefault()): CountryOption? {
    val code = isoCode?.trim()?.uppercase().orEmpty()
    if (code.length != 2) return null
    val count = stationCount ?: 0
    if (count <= 0) return null
    if (!recognizedCountryCode(code)) return null
    val fallback = name?.trim().orEmpty().ifBlank { code }
    return CountryOption(
        code = code,
        name = localizedCountryName(code, fallback, locale),
        stationCount = count,
    )
}

/** Radio-Browser uses codes such as XX for unknown stations. Locale then returns the code itself. */
internal fun recognizedCountryCode(code: String): Boolean {
    if (code.length != 2) return false
    val english = Locale("", code).getDisplayCountry(Locale.ENGLISH)
    return english.isNotBlank() && !english.equals(code, ignoreCase = true)
}

fun localizedCountryName(code: String, fallback: String, locale: Locale = Locale.getDefault()): String {
    if (code.length != 2) return fallback
    val display = Locale("", code).getDisplayCountry(locale)
    return display.ifBlank { fallback }
}
