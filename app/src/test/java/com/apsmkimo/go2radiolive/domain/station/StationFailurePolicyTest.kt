package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StationFailurePolicyTest {
    private val now = 1_700_000_000_000L

    @Test
    fun repeatedFailuresSinkToTheBottomWithoutHiding() {
        val stations = listOf(row("a"), row("b"), row("c"))
        val once = mapOf("b" to PlayFailureRecord(consecutive = 1, lastFailureAtMs = now))
        val twice = mapOf("b" to PlayFailureRecord(consecutive = 2, lastFailureAtMs = now))
        val thrice = mapOf("b" to PlayFailureRecord(consecutive = 3, lastFailureAtMs = now))
        assertEquals(listOf("a", "b", "c"), ids(StationFailurePolicy.sink(stations, once, now)))
        assertEquals(listOf("a", "b", "c"), ids(StationFailurePolicy.sink(stations, twice, now)))
        assertEquals(listOf("a", "c", "b"), ids(StationFailurePolicy.sink(stations, thrice, now)))
        assertFalse(StationFailurePolicy.shouldSink(twice.getValue("b"), now))
        assertTrue(StationFailurePolicy.shouldSink(thrice.getValue("b"), now))
    }

    @Test
    fun successAndTimeRestoreAStation() {
        var record: PlayFailureRecord? = null
        repeat(3) { record = StationFailurePolicy.failed(record, now) }
        assertTrue(StationFailurePolicy.shouldSink(record, now))
        val played = StationFailurePolicy.succeeded(record, now + 1_000)
        assertFalse(StationFailurePolicy.shouldSink(played, now + 1_000))
        assertEquals(0, played.consecutive)

        val stale = PlayFailureRecord(
            consecutive = 5,
            lastFailureAtMs = now - StationFailurePolicy.RECOVER_AFTER_MS - 1,
        )
        assertFalse(StationFailurePolicy.shouldSink(stale, now))
        val again = StationFailurePolicy.failed(stale, now)
        assertEquals(1, again.consecutive)
        assertFalse(StationFailurePolicy.shouldSink(again, now))
    }

    private fun row(id: String) = RadioStation(stationUuid = id, name = id, streamUrl = "https://example.test/$id")

    private fun ids(stations: List<RadioStation>) = stations.map { it.stationUuid }
}
