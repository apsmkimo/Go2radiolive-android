package com.apsmkimo.go2radiolive.data.mapper

import com.apsmkimo.go2radiolive.data.remote.dto.CountryDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MappersTest {
    @Test
    fun countryMappingDropsUnknownCodesAndKeepsRealOnes() {
        assertNull(CountryDto(name = "", isoCode = "XX", stationCount = 1).toDomain(Locale.ENGLISH))

        val taiwan = CountryDto(name = "Taiwan", isoCode = "TW", stationCount = 10).toDomain(Locale.TAIWAN)
        assertEquals("TW", taiwan?.code)
        assertNotNull(taiwan)
        assertFalse(taiwan!!.name.equals("TW", ignoreCase = true))

        val kosovo = CountryDto(name = "Kosovo", isoCode = "XK", stationCount = 18).toDomain(Locale.ENGLISH)
        assertEquals("XK", kosovo?.code)
        assertNotNull(kosovo)
        assertFalse(kosovo!!.name.equals("XK", ignoreCase = true))

        val unitedNations = CountryDto(name = "", isoCode = "UN", stationCount = 1).toDomain(Locale.ENGLISH)
        assertEquals("UN", unitedNations?.code)
        assertFalse(unitedNations!!.name.equals("UN", ignoreCase = true))
    }
}
