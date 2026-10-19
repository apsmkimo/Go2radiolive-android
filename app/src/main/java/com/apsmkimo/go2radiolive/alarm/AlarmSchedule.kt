package com.apsmkimo.go2radiolive.alarm

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Next wake-up instant. An empty [repeatDays] set is a one-time alarm (today, or tomorrow
 * if that clock time has already passed). A non-empty set fires on the next matching day
 * strictly after [nowMillis].
 */
object AlarmSchedule {
    const val SNOOZE_MS = 10L * 60L * 1000L
    const val FADE_MS = 30_000L

    fun snoozeAt(nowMillis: Long): Long = nowMillis + SNOOZE_MS

    fun nextTriggerMillis(
        nowMillis: Long,
        zone: ZoneId,
        hour: Int,
        minute: Int,
        repeatDays: Set<DayOfWeek>,
    ): Long {
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        val time = LocalTime.of(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
        if (repeatDays.isEmpty()) {
            return oneTime(now, time).toInstant().toEpochMilli()
        }
        for (offset in 0..7) {
            val date = now.toLocalDate().plusDays(offset.toLong())
            if (date.dayOfWeek !in repeatDays) continue
            val candidate = date.atTime(time).atZone(zone)
            if (candidate.toInstant().isAfter(now.toInstant())) {
                return candidate.toInstant().toEpochMilli()
            }
        }
        val fallback = now.toLocalDate().plusDays(7).atTime(time).atZone(zone)
        return fallback.toInstant().toEpochMilli()
    }

    private fun oneTime(now: ZonedDateTime, time: LocalTime): ZonedDateTime {
        var candidate = now.toLocalDate().atTime(time).atZone(now.zone)
        if (!candidate.toInstant().isAfter(now.toInstant())) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }
}
