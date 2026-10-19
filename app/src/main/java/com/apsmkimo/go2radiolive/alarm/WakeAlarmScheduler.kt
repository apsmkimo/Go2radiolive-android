package com.apsmkimo.go2radiolive.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.presentation.MainActivity
import com.google.gson.Gson
import java.time.ZoneId

object WakeAlarmScheduler {
    const val ACTION_FIRE = "com.apsmkimo.go2radiolive.ALARM_FIRE"
    const val ACTION_SNOOZE_FIRE = "com.apsmkimo.go2radiolive.ALARM_SNOOZE_FIRE"
    const val EXTRA_STATION_JSON = "extra_station_json"
    private const val REQUEST_MAIN = 4101
    private const val REQUEST_SNOOZE = 4102
    private const val REQUEST_SHOW = 4103

    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = alarmManager(context) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun exactAlarmSettingsIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }

    fun apply(context: Context, alarm: WakeAlarm) {
        if (!alarm.enabled || alarm.station == null || alarm.station.streamUrl.isBlank()) {
            cancelMain(context)
            return
        }
        val trigger = AlarmSchedule.nextTriggerMillis(
            nowMillis = System.currentTimeMillis(),
            zone = ZoneId.systemDefault(),
            hour = alarm.hour,
            minute = alarm.minute,
            repeatDays = alarm.repeatDaySet(),
        )
        scheduleMain(context, trigger)
    }

    fun scheduleSnooze(context: Context, station: RadioStation, gson: Gson) {
        val trigger = AlarmSchedule.snoozeAt(System.currentTimeMillis())
        val intent = Intent(context, WakeAlarmFireReceiver::class.java).apply {
            action = ACTION_SNOOZE_FIRE
            putExtra(EXTRA_STATION_JSON, gson.toJson(station))
        }
        val pending = pendingBroadcast(context, REQUEST_SNOOZE, intent)
        setClock(context, trigger, pending)
    }

    fun cancelAll(context: Context) {
        cancelMain(context)
        val snooze = pendingBroadcast(
            context,
            REQUEST_SNOOZE,
            Intent(context, WakeAlarmFireReceiver::class.java).apply { action = ACTION_SNOOZE_FIRE },
        )
        alarmManager(context)?.cancel(snooze)
    }

    private fun scheduleMain(context: Context, trigger: Long) {
        val intent = Intent(context, WakeAlarmFireReceiver::class.java).apply { action = ACTION_FIRE }
        setClock(context, trigger, pendingBroadcast(context, REQUEST_MAIN, intent))
    }

    private fun cancelMain(context: Context) {
        val intent = Intent(context, WakeAlarmFireReceiver::class.java).apply { action = ACTION_FIRE }
        alarmManager(context)?.cancel(pendingBroadcast(context, REQUEST_MAIN, intent))
    }

    private fun setClock(context: Context, trigger: Long, operation: PendingIntent) {
        val alarmManager = alarmManager(context) ?: return
        val show = PendingIntent.getActivity(
            context,
            REQUEST_SHOW,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            if (canScheduleExact(context)) {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), operation)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, operation)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, trigger, operation)
        }
    }

    private fun pendingBroadcast(context: Context, request: Int, intent: Intent): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun alarmManager(context: Context): AlarmManager? {
        return context.getSystemService(AlarmManager::class.java)
    }
}
