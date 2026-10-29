package com.apsmkimo.go2radiolive.domain.station

import com.apsmkimo.go2radiolive.domain.model.RadioStation

/**
 * On-device playback failures. Nothing here is sent to a server.
 * Three consecutive failures sink a station to the bottom of a list.
 * A later success, or a week without a new failure, brings it back.
 */
data class PlayFailureRecord(
    val consecutive: Int = 0,
    val lastFailureAtMs: Long = 0,
    val lastSuccessAtMs: Long = 0,
)

object StationFailurePolicy {
    const val SINK_AFTER = 3
    const val RECOVER_AFTER_MS: Long = 7L * 24 * 60 * 60 * 1000

    fun failed(previous: PlayFailureRecord?, nowMs: Long): PlayFailureRecord {
        val expired = previous != null && nowMs - previous.lastFailureAtMs >= RECOVER_AFTER_MS
        val recovered = previous != null && previous.lastSuccessAtMs > previous.lastFailureAtMs
        val count = if (previous == null || expired || recovered) 1 else previous.consecutive + 1
        return PlayFailureRecord(
            consecutive = count,
            lastFailureAtMs = nowMs,
            lastSuccessAtMs = previous?.lastSuccessAtMs ?: 0,
        )
    }

    fun succeeded(previous: PlayFailureRecord?, nowMs: Long): PlayFailureRecord {
        return PlayFailureRecord(
            consecutive = 0,
            lastFailureAtMs = previous?.lastFailureAtMs ?: 0,
            lastSuccessAtMs = nowMs,
        )
    }

    fun shouldSink(record: PlayFailureRecord?, nowMs: Long): Boolean {
        if (record == null) return false
        if (record.consecutive < SINK_AFTER) return false
        if (record.lastSuccessAtMs > record.lastFailureAtMs) return false
        if (nowMs - record.lastFailureAtMs >= RECOVER_AFTER_MS) return false
        return true
    }

    fun sink(
        stations: List<RadioStation>,
        records: Map<String, PlayFailureRecord>,
        nowMs: Long,
    ): List<RadioStation> {
        if (stations.size < 2 || records.isEmpty()) return stations
        val head = ArrayList<RadioStation>(stations.size)
        val tail = ArrayList<RadioStation>()
        for (station in stations) {
            if (shouldSink(records[station.stationUuid], nowMs)) tail.add(station) else head.add(station)
        }
        if (tail.isEmpty()) return stations
        head.addAll(tail)
        return head
    }
}
