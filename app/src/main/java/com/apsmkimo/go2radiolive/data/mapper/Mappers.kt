package com.apsmkimo.go2radiolive.data.mapper

import com.apsmkimo.go2radiolive.data.remote.dto.CountryDto
import com.apsmkimo.go2radiolive.data.remote.dto.StationDto
import com.apsmkimo.go2radiolive.data.remote.dto.TagDto
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.station.StationLists
import java.util.Locale

fun StationDto.toDomain(): RadioStation? {
    val uuid = stationUuid?.trim().orEmpty()
    val displayName = name?.trim().orEmpty()
    // SMCPKG_SUPPORT>>>Cursor119
    // val stream = urlResolved?.trim().takeUnless { it.isNullOrEmpty() }
    //     ?: url?.trim().orEmpty()
    // if (uuid.isEmpty() || displayName.isEmpty() || stream.isEmpty()) return null
    val resolved = urlResolved?.trim().orEmpty()
    val raw = url?.trim().orEmpty()
    val stream = resolved.ifEmpty { raw }
    if (uuid.isEmpty() || displayName.isEmpty() || stream.isEmpty()) return null
    val alternates = listOf(raw, resolved).filter { it.isNotEmpty() && it != stream }.distinct()
    val checkedAt = StationLists.parseCheckTime(lastCheckOkTimeIso)
        ?: StationLists.parseCheckTime(lastCheckTimeIso)
        ?: StationLists.parseCheckTime(lastCheckOkTime)
        ?: StationLists.parseCheckTime(lastCheckTime)
        ?: 0L
    val station = RadioStation(
        // SMCPKG_SUPPORT<<<Cursor119
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
        // SMCPKG_SUPPORT>>>Cursor119
        alternateUrls = alternates,
        lastCheckOk = lastCheckOk,
        lastCheckTimeMs = checkedAt,
        codec = codec,
        sslError = sslError == 1,
        hls = hls == 1,
    )
    return station.takeIf { StationLists.isListable(it) }
    // SMCPKG_SUPPORT<<<Cursor119
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
    // SMCPKG_SUPPORT>>>Cursor119
    // mapNotNull { it.toDomain() }
    //     .distinctBy { it.stationUuid }
    StationLists.dedupe(mapNotNull { it.toDomain() })
    // SMCPKG_SUPPORT<<<Cursor119

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
