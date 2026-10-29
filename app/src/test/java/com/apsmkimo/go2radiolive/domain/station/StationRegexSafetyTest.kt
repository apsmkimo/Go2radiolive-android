package com.apsmkimo.go2radiolive.domain.station

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StationRegexSafetyTest {
    @Test
    fun stationAndPlaylistPatternsEscapeLiteralBraces() {
        val sources = StationLists.patternSources + PlaylistParser.patternSources
        assertTrue(sources.isNotEmpty())
        for (source in sources) {
            assertEquals(source, emptyList<Int>(), IcuPatternRules.illegalBraceIndexes(source))
            assertNotNull(source, IcuPatternRules.compile(source))
        }
    }

    @Test
    fun theLaunchCrashPatternIsIllegalOnIcu() {
        val broken = """\([^)]*\)|\[[^\]]*\]|\{[^}]*}|【[^】]*】|「[^」]*」|『[^』]*』"""
        assertTrue(IcuPatternRules.illegalBraceIndexes(broken).isNotEmpty())
        assertTrue(StationLists.BRACKETS_PATTERN.contains("""\{[^}]*\}"""))
    }

    @Test
    fun curlyAndSquareBracketsAreRemovedFromTheNameKey() {
        assertEquals("kiss", StationLists.nameKey("Kiss {HD}"))
        assertEquals("kiss", StationLists.nameKey("Kiss [Live]"))
    }
}
