package com.apsmkimo.go2radiolive.presentation.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.alarm.AlarmSchedule
import com.apsmkimo.go2radiolive.alarm.PlaybackSignals
import com.apsmkimo.go2radiolive.alarm.SleepTimerState
import com.apsmkimo.go2radiolive.alarm.WakeAlarmScheduler
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.locale.ALARM_CHANNEL_ID
import com.apsmkimo.go2radiolive.locale.withAppLocale
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Sleep timer and wake-up playback. Both run on the playback service so they continue
 * with the screen off while the player holds its wake lock.
 */
internal class AlarmPlayback(
    private val service: Service,
    private val player: ExoPlayer,
    private val gson: Gson,
    private val scope: CoroutineScope,
) {
    private var sleepJob: Job? = null
    private var rampJob: Job? = null
    private var soundJob: Job? = null
    private var ringtone: Ringtone? = null
    private var alarmSession = false
    private var expectTransition = false
    private var primaryTries = 0
    private var usingBackup = false
    private var primary: RadioStation? = null
    private var backup: RadioStation? = null
    private val notificationId = 2002
    private var lastNotificationText: String? = null

    fun refreshLocale() {
        ensureChannel()
        val text = lastNotificationText ?: return
        if (alarmSession) showNotification(text)
    }

    fun attach() {
        ensureChannel()
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (alarmSession) onAlarmStreamFailed()
            }

            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (expectTransition) {
                    expectTransition = false
                    return
                }
                if (alarmSession) {
                    alarmSession = false
                    cancelNotification()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val sleep = PlaybackSignals.sleep.value
                if (sleep.active && sleep.endOfEpisode && playbackState == Player.STATE_ENDED) {
                    finishSleep()
                }
            }
        })
    }

    fun handle(intent: Intent?) {
        when (intent?.action) {
            RadioPlaybackService.ACTION_SLEEP_MINUTES -> {
                val minutes = intent.getIntExtra(RadioPlaybackService.EXTRA_MINUTES, 0)
                if (minutes > 0) startMinutes(minutes)
            }
            RadioPlaybackService.ACTION_SLEEP_EPISODE -> startEndOfEpisode()
            RadioPlaybackService.ACTION_SLEEP_CANCEL -> cancelSleep(restoreVolume = true)
            RadioPlaybackService.ACTION_ALARM_PLAY -> startAlarm(intent)
            RadioPlaybackService.ACTION_ALARM_STOP -> stopAlarm()
            RadioPlaybackService.ACTION_ALARM_SNOOZE -> snooze()
        }
    }

    fun release() {
        sleepJob?.cancel()
        rampJob?.cancel()
        soundJob?.cancel()
        ringtone?.stop()
        ringtone = null
    }

    private fun startMinutes(minutes: Int) {
        cancelSleep(restoreVolume = false)
        val end = android.os.SystemClock.elapsedRealtime() + minutes * 60_000L
        PlaybackSignals.updateSleep(SleepTimerState(active = true, endsAtElapsedMs = end))
        sleepJob = scope.launch {
            val fadeAt = end - AlarmSchedule.FADE_MS
            val wait = fadeAt - android.os.SystemClock.elapsedRealtime()
            if (wait > 0) delay(wait)
            if (!isActive) return@launch
            fadeOutAndPause()
            PlaybackSignals.updateSleep(SleepTimerState())
        }
    }

    private fun startEndOfEpisode() {
        val current = currentStation()
        if (current?.directStream != true) return
        cancelSleep(restoreVolume = false)
        PlaybackSignals.updateSleep(SleepTimerState(active = true, endOfEpisode = true))
        sleepJob = scope.launch {
            while (isActive) {
                val duration = player.duration
                val position = player.currentPosition
                val known = duration > 0 && duration != androidx.media3.common.C.TIME_UNSET
                if (known && duration - position <= AlarmSchedule.FADE_MS) {
                    fadeOutAndPause()
                    break
                }
                if (player.playbackState == Player.STATE_ENDED) {
                    player.pause()
                    player.volume = 1f
                    break
                }
                delay(500)
            }
            PlaybackSignals.updateSleep(SleepTimerState())
        }
    }

    private fun cancelSleep(restoreVolume: Boolean) {
        sleepJob?.cancel()
        sleepJob = null
        if (restoreVolume) player.volume = 1f
        PlaybackSignals.updateSleep(SleepTimerState())
    }

    private fun finishSleep() {
        sleepJob?.cancel()
        player.pause()
        player.volume = 1f
        PlaybackSignals.updateSleep(SleepTimerState())
    }

    private suspend fun fadeOutAndPause() {
        rampJob?.cancel()
        val start = player.volume
        val steps = 30
        for (step in 1..steps) {
            player.volume = start * (1f - step / steps.toFloat())
            delay(AlarmSchedule.FADE_MS / steps)
        }
        player.pause()
        player.volume = 1f
    }

    private fun startAlarm(intent: Intent) {
        val json = intent.getStringExtra(WakeAlarmScheduler.EXTRA_STATION_JSON) ?: return
        val station = runCatching { gson.fromJson(json, RadioStation::class.java) }.getOrNull() ?: return
        val app = service.application as? Go2RadioApp
        val stored = app?.container?.wakeAlarmRepository?.alarm?.value
        primary = station
        backup = stored?.backup?.takeIf { it.streamUrl.isNotBlank() && it.stationUuid != station.stationUuid }
        primaryTries = 1
        usingBackup = false
        alarmSession = true
        app?.container?.wakeAlarmRepository?.remember(station)
        showNotification(station.name)
        play(station, ramp = true)
    }

    private fun onAlarmStreamFailed() {
        if (!alarmSession) return
        val mainStation = primary
        if (!usingBackup && primaryTries < 2 && mainStation != null) {
            primaryTries++
            play(mainStation, ramp = false)
            return
        }
        val fallback = backup
        if (!usingBackup && fallback != null) {
            usingBackup = true
            play(fallback, ramp = true)
            showNotification(fallback.name)
            return
        }
        playFallbackSound()
    }

    private fun play(station: RadioStation, ramp: Boolean) {
        val item = stationMediaItem(service, gson, station, station.streamUrl)
        expectTransition = true
        rampJob?.cancel()
        player.volume = if (ramp) 0f else 1f
        player.setMediaItem(item)
        player.prepare()
        player.play()
        if (ramp) {
            rampJob = scope.launch { fadeIn() }
        }
    }

    private suspend fun fadeIn() {
        val steps = 30
        for (step in 1..steps) {
            player.volume = step / steps.toFloat()
            delay(AlarmSchedule.FADE_MS / steps)
        }
        player.volume = 1f
    }

    private fun stopAlarm() {
        alarmSession = false
        rampJob?.cancel()
        soundJob?.cancel()
        ringtone?.stop()
        ringtone = null
        player.pause()
        player.volume = 1f
        cancelNotification()
    }

    private fun snooze() {
        val station = primary ?: currentStation()
        stopAlarm()
        if (station != null && station.streamUrl.isNotBlank()) {
            WakeAlarmScheduler.scheduleSnooze(service, station, gson)
        }
        val manager = service.getSystemService(NotificationManager::class.java)
        manager.notify(
            notificationId,
            buildNotification(service.withAppLocale().getString(R.string.alarm_snoozed), ongoing = false),
        )
    }

    private fun playFallbackSound() {
        alarmSession = false
        rampJob?.cancel()
        player.pause()
        player.volume = 1f
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone?.stop()
        ringtone = uri?.let { RingtoneManager.getRingtone(service, it) }
        ringtone?.play()
        showNotification(service.withAppLocale().getString(R.string.alarm_failed))
        soundJob = scope.launch {
            delay(30_000)
            ringtone?.stop()
            ringtone = null
            cancelNotification()
        }
    }

    private fun currentStation(): RadioStation? {
        val json = player.currentMediaItem?.mediaMetadata?.extras?.getString(EXTRA_STATION_JSON)
        return json?.let { runCatching { gson.fromJson(it, RadioStation::class.java) }.getOrNull() }
    }

    private fun showNotification(text: String) {
        lastNotificationText = text
        val notification = buildNotification(text, ongoing = true)
        if (Build.VERSION.SDK_INT >= 29) {
            service.startForeground(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        } else {
            service.startForeground(notificationId, notification)
        }
    }

    private fun cancelNotification() {
        service.getSystemService(NotificationManager::class.java).cancel(notificationId)
    }

    private fun buildNotification(text: String, ongoing: Boolean): Notification {
        val stop = serviceIntent(RadioPlaybackService.ACTION_ALARM_STOP, 1)
        val snooze = serviceIntent(RadioPlaybackService.ACTION_ALARM_SNOOZE, 2)
        val localized = service.withAppLocale()
        return NotificationCompat.Builder(service, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(localized.getString(R.string.alarm_playing))
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .addAction(0, localized.getString(R.string.alarm_stop), stop)
            .addAction(0, localized.getString(R.string.alarm_snooze), snooze)
            .build()
    }

    private fun serviceIntent(action: String, request: Int): PendingIntent {
        val intent = Intent(service, RadioPlaybackService::class.java).apply { this.action = action }
        return PendingIntent.getService(
            service,
            request,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = service.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            service.withAppLocale().getString(R.string.alarm_channel),
            NotificationManager.IMPORTANCE_HIGH,
        )
        manager.createNotificationChannel(channel)
    }

}
