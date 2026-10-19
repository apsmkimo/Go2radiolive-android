package com.apsmkimo.go2radiolive.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.ads.AppOpenAds
import com.apsmkimo.go2radiolive.presentation.player.RadioPlaybackService
import kotlinx.coroutines.runBlocking

/** Explicit alarms only. Not exported. */
class WakeAlarmFireReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Thread {
            try {
                deliver(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }.start()
    }

    private fun deliver(context: Context, intent: Intent) {
        val app = context as? Go2RadioApp ?: return
        val repo = app.container.wakeAlarmRepository
        runBlocking { repo.awaitLoaded() }
        val gson = app.container.gson
        when (intent.action) {
            WakeAlarmScheduler.ACTION_FIRE -> {
                val alarm = repo.alarm.value
                val station = alarm.station ?: return
                if (!alarm.enabled || station.streamUrl.isBlank()) return
                AppOpenAds.suppressNextShow()
                startPlayback(context, gson.toJson(station))
                if (alarm.repeatDays.isEmpty()) {
                    runBlocking { repo.save(alarm.copy(enabled = false)) }
                    WakeAlarmScheduler.apply(context, repo.alarm.value)
                } else {
                    WakeAlarmScheduler.apply(context, alarm)
                }
            }
            WakeAlarmScheduler.ACTION_SNOOZE_FIRE -> {
                val json = intent.getStringExtra(WakeAlarmScheduler.EXTRA_STATION_JSON) ?: return
                AppOpenAds.suppressNextShow()
                startPlayback(context, json)
            }
        }
    }

    private fun startPlayback(context: Context, stationJson: String) {
        val start = Intent(context, RadioPlaybackService::class.java).apply {
            action = RadioPlaybackService.ACTION_ALARM_PLAY
            putExtra(WakeAlarmScheduler.EXTRA_STATION_JSON, stationJson)
        }
        ContextCompat.startForegroundService(context, start)
    }
}

/** Boot and clock changes. Exported for system broadcasts only. */
class WakeAlarmSystemReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> Unit
            else -> return
        }
        val pending = goAsync()
        Thread {
            try {
                val app = context.applicationContext as? Go2RadioApp ?: return@Thread
                val repo = app.container.wakeAlarmRepository
                runBlocking { repo.awaitLoaded() }
                WakeAlarmScheduler.apply(context.applicationContext, repo.alarm.value)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
