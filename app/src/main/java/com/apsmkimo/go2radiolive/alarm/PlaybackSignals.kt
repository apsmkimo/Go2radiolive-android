package com.apsmkimo.go2radiolive.alarm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Process-wide sleep-timer state shared by the playback service and the UI. */
object PlaybackSignals {
    private val _sleep = MutableStateFlow(SleepTimerState())
    val sleep: StateFlow<SleepTimerState> = _sleep.asStateFlow()

    fun updateSleep(state: SleepTimerState) {
        _sleep.value = state
    }
}
