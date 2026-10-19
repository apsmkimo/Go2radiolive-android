package com.apsmkimo.go2radiolive.alarm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmScheduleTest {
    private val zone = ZoneId.of("Asia/Taipei")

    @Test
    fun oneTimeUsesLaterToday() {
        val now = at(2026, 10, 19, 8, 0)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 9, 30, emptySet())
        assertEquals(at(2026, 10, 19, 9, 30), trigger)
    }

    @Test
    fun oneTimeRollsToTomorrowAfterTheClockTime() {
        val now = at(2026, 10, 19, 8, 0)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 7, 0, emptySet())
        assertEquals(at(2026, 10, 20, 7, 0), trigger)
    }

    @Test
    fun oneTimeAtTheExactInstantMovesToTheNextDay() {
        val now = at(2026, 10, 19, 7, 0)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 7, 0, emptySet())
        assertEquals(at(2026, 10, 20, 7, 0), trigger)
    }

    @Test
    fun repeatingUsesTheNextSelectedDay() {
        val now = at(2026, 10, 19, 8, 0)
        val tuesday = setOf(DayOfWeek.TUESDAY)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 7, 15, tuesday)
        assertEquals(at(2026, 10, 20, 7, 15), trigger)
    }

    @Test
    fun repeatingSkipsTodayWhenThatTimeHasPassed() {
        val now = at(2026, 10, 19, 8, 0)
        val monday = setOf(DayOfWeek.MONDAY)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 7, 0, monday)
        assertEquals(at(2026, 10, 26, 7, 0), trigger)
    }

    @Test
    fun repeatingKeepsTodayWhenTheTimeIsStillAhead() {
        val now = at(2026, 10, 19, 8, 0)
        val monday = setOf(DayOfWeek.MONDAY)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 9, 0, monday)
        assertEquals(at(2026, 10, 19, 9, 0), trigger)
    }

    @Test
    fun triggerIsAlwaysAfterNow() {
        val now = at(2026, 10, 31, 23, 50)
        val trigger = AlarmSchedule.nextTriggerMillis(now, zone, 0, 10, setOf(DayOfWeek.SUNDAY))
        assertTrue(trigger > now)
    }

    @Test
    fun snoozeIsTenMinutesLater() {
        val now = 1_700_000_000_000L
        assertEquals(now + 10L * 60L * 1000L, AlarmSchedule.snoozeAt(now))
    }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(year, month, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()
    }
}
