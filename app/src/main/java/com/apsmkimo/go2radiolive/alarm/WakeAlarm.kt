package com.apsmkimo.go2radiolive.alarm

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import java.time.DayOfWeek

/** Persisted wake-up alarm. Empty [repeatDays] means one time. Values are [DayOfWeek.getValue]. */
data class WakeAlarm(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val repeatDays: List<Int> = emptyList(),
    val station: RadioStation? = null,
    val backup: RadioStation? = null,
) {
    fun repeatDaySet(): Set<DayOfWeek> {
        return repeatDays.mapNotNull { value ->
            DayOfWeek.entries.firstOrNull { it.value == value }
        }.toSet()
    }
}

data class SleepTimerState(
    val active: Boolean = false,
    val endsAtElapsedMs: Long = 0L,
    val endOfEpisode: Boolean = false,
)
