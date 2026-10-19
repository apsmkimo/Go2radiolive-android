@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.apsmkimo.go2radiolive.presentation.ui.components

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.alarm.SleepTimerState
import com.apsmkimo.go2radiolive.alarm.WakeAlarm
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val SLEEP_PRESETS = intArrayOf(15, 30, 45, 60, 90)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmSheet(
    alarm: WakeAlarm,
    lastStation: RadioStation?,
    favorites: List<RadioStation>,
    playback: PlaybackState,
    sleep: SleepTimerState,
    canExactAlarm: Boolean,
    onDismiss: () -> Unit,
    onSaveAlarm: (WakeAlarm) -> Unit,
    onStartSleepMinutes: (Int) -> Unit,
    onStartSleepEpisode: () -> Unit,
    onCancelSleep: () -> Unit,
    onRequestExactPermission: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var page by remember { mutableIntStateOf(0) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.alarm_sheet))
            TabRow(selectedTabIndex = page) {
                Tab(selected = page == 0, onClick = { page = 0 }, text = { Text(stringResource(R.string.sleep_timer)) })
                Tab(selected = page == 1, onClick = { page = 1 }, text = { Text(stringResource(R.string.alarm_clock)) })
            }
            if (page == 0) {
                SleepSection(
                    playback = playback,
                    sleep = sleep,
                    onStartSleepMinutes = onStartSleepMinutes,
                    onStartSleepEpisode = onStartSleepEpisode,
                    onCancelSleep = onCancelSleep,
                )
            } else {
                AlarmSection(
                    alarm = alarm,
                    lastStation = lastStation,
                    favorites = favorites,
                    canExactAlarm = canExactAlarm,
                    onSaveAlarm = onSaveAlarm,
                    onRequestExactPermission = onRequestExactPermission,
                )
            }
        }
    }
}

@Composable
private fun SleepSection(
    playback: PlaybackState,
    sleep: SleepTimerState,
    onStartSleepMinutes: (Int) -> Unit,
    onStartSleepEpisode: () -> Unit,
    onCancelSleep: () -> Unit,
) {
    var custom by remember { mutableStateOf("") }
    val remaining = sleepRemainingLabel(sleep)
    if (sleep.active && remaining != null) {
        Text(remaining)
        OutlinedButton(onClick = onCancelSleep, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sleep_cancel))
        }
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SLEEP_PRESETS.forEach { minutes ->
            FilterChip(
                selected = false,
                onClick = { onStartSleepMinutes(minutes) },
                label = { Text(stringResource(R.string.sleep_minutes, minutes)) },
            )
        }
    }
    OutlinedTextField(
        value = custom,
        onValueChange = { custom = it.filter { ch -> ch.isDigit() }.take(3) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(stringResource(R.string.sleep_custom_hint), maxLines = 1) },
        placeholder = { Text(stringResource(R.string.sleep_custom)) },
    )
    Button(
        onClick = { custom.toIntOrNull()?.takeIf { it in 1..240 }?.let(onStartSleepMinutes) },
        enabled = custom.toIntOrNull() in 1..240,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.sleep_custom))
    }
    val episodeEnabled = playback.station?.directStream == true && (playback.isPlaying || playback.isBuffering)
    OutlinedButton(
        onClick = onStartSleepEpisode,
        enabled = episodeEnabled,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.sleep_end_episode))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AlarmSection(
    alarm: WakeAlarm,
    lastStation: RadioStation?,
    favorites: List<RadioStation>,
    canExactAlarm: Boolean,
    onSaveAlarm: (WakeAlarm) -> Unit,
    onRequestExactPermission: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales.let { if (it.isEmpty) Locale.getDefault() else it[0] }
    val timeState = rememberTimePickerState(initialHour = alarm.hour, initialMinute = alarm.minute, is24Hour = true)
    var days by remember(alarm.repeatDays) { mutableStateOf(alarm.repeatDays.toSet()) }
    var enabled by remember(alarm.enabled) { mutableStateOf(alarm.enabled) }
    val options = remember(lastStation, favorites, alarm.station) {
        buildList {
            alarm.station?.let { add(it) }
            lastStation?.let { if (none { station -> station.stationUuid == it.stationUuid }) add(it) }
            favorites.forEach { station ->
                if (none { existing -> existing.stationUuid == station.stationUuid }) add(station)
            }
        }
    }
    var selectedId by remember(options, alarm.station, lastStation) {
        mutableStateOf(
            alarm.station?.stationUuid
                ?: lastStation?.stationUuid
                ?: options.firstOrNull()?.stationUuid
                ?: "",
        )
    }
    TimePicker(state = timeState)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = days.isEmpty(),
            onClick = { days = emptySet() },
            label = { Text(stringResource(R.string.alarm_once)) },
        )
        FilterChip(
            selected = days == WEEKDAYS,
            onClick = { days = WEEKDAYS },
            label = { Text(stringResource(R.string.alarm_weekdays)) },
        )
        DayOfWeek.entries.forEach { day ->
            FilterChip(
                selected = day.value in days,
                onClick = {
                    days = if (day.value in days) days - day.value else days + day.value
                },
                label = { Text(day.getDisplayName(TextStyle.SHORT, locale)) },
            )
        }
    }
    Text(stringResource(R.string.alarm_station))
    if (options.isEmpty()) {
        Text(stringResource(R.string.alarm_no_station))
    } else {
        options.forEach { station ->
            FilterChip(
                selected = station.stationUuid == selectedId,
                onClick = { selectedId = station.stationUuid },
                label = { Text(station.name, maxLines = 1) },
            )
        }
    }
    androidx.compose.foundation.layout.Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.alarm_enable))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
    if (enabled && !canExactAlarm) {
        Text(stringResource(R.string.alarm_permission))
        TextButton(onClick = onRequestExactPermission) {
            Text(stringResource(R.string.alarm_permission_action))
        }
    }
    Button(
        onClick = {
            val selected = options.firstOrNull { it.stationUuid == selectedId }
            val backup = options.firstOrNull { it.stationUuid != selected?.stationUuid }
            onSaveAlarm(
                WakeAlarm(
                    enabled = enabled && selected != null,
                    hour = timeState.hour,
                    minute = timeState.minute,
                    repeatDays = days.sorted(),
                    station = selected,
                    backup = backup,
                ),
            )
        },
        enabled = options.any { it.stationUuid == selectedId },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(stringResource(R.string.alarm_save))
    }
}

@Composable
fun sleepRemainingLabel(sleep: SleepTimerState): String? {
    if (!sleep.active) return null
    if (sleep.endOfEpisode) return stringResource(R.string.sleep_end_episode)
    var now by remember(sleep.endsAtElapsedMs) { mutableStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sleep.endsAtElapsedMs) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val remain = (sleep.endsAtElapsedMs - now).coerceAtLeast(0L)
    val totalSeconds = remain / 1000
    val text = "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    return stringResource(R.string.sleep_remaining, text)
}

fun sleepBadgeMinutes(sleep: SleepTimerState, nowElapsed: Long): String? {
    if (!sleep.active) return null
    if (sleep.endOfEpisode) return "EP"
    val remain = (sleep.endsAtElapsedMs - nowElapsed).coerceAtLeast(0L)
    val minutes = ((remain + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    return minutes.toString()
}

private val WEEKDAYS = setOf(
    DayOfWeek.MONDAY.value,
    DayOfWeek.TUESDAY.value,
    DayOfWeek.WEDNESDAY.value,
    DayOfWeek.THURSDAY.value,
    DayOfWeek.FRIDAY.value,
)
