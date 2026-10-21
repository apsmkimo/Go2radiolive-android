package com.apsmkimo.go2radiolive.presentation.player

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.audio.dsp.Go2RenderersFactory
import com.apsmkimo.go2radiolive.audio.spatial.SpatialRoute
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import com.apsmkimo.go2radiolive.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val spatialRouteScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var spatialRouteJob: Job? = null
    private var alarmPlayback: AlarmPlayback? = null

    override fun onCreate() {
        super.onCreate()
        // SMCPKG_SUPPORT>>>Cursor011
        AudioSettingsRepository.start(this)
        // SMCPKG_SUPPORT<<<Cursor011
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)

        // SMCPKG_SUPPORT>>>Cursor012
        // val player = ExoPlayer.Builder(this)
        //     .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        //     .setAudioAttributes(
        val player = ExoPlayer.Builder(this)
            .setRenderersFactory(Go2RenderersFactory(this))
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        // SMCPKG_SUPPORT<<<Cursor012
        player.repeatMode = Player.REPEAT_MODE_OFF
        // SMCPKG_SUPPORT>>>Cursor098
        alarmPlayback = AlarmPlayback(
            service = this,
            player = player,
            gson = com.google.gson.Gson(),
            scope = spatialRouteScope,
        ).also { it.attach() }
        // SMCPKG_SUPPORT<<<Cursor098

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .build()
        // SMCPKG_SUPPORT>>>Cursor072
        // Mono Off reports one channel and skips this processor. Media3 keeps
        // that format until the sink is configured again, so a mono stream
        // fades out, reloads the current item, and fades back in.
        spatialRouteJob = spatialRouteScope.launch {
            var previousOff: Boolean? = null
            AudioSettingsRepository.settings.collect { settings ->
                val off = settings.spatialMode == SpatialAudioMode.OFF
                val wasOff = previousOff
                previousOff = off
                if (wasOff == null || wasOff == off) return@collect
                if (SpatialRoute.inputChannels != 1) return@collect
                if (player.mediaItemCount == 0 || player.currentMediaItem == null) return@collect
                fadeAcrossBypass(player)
            }
        }
        // SMCPKG_SUPPORT<<<Cursor072
    }

    private suspend fun fadeAcrossBypass(player: ExoPlayer) {
        val item = player.currentMediaItem ?: return
        val resume = player.playWhenReady
        fadeVolume(player, 0f)
        try {
            player.setMediaItem(item, /* resetPosition = */ false)
            player.prepare()
            player.playWhenReady = resume
        } finally {
            fadeVolume(player, 1f)
        }
    }

    private suspend fun fadeVolume(player: ExoPlayer, target: Float) {
        val start = player.volume
        var step = 1
        while (step <= VOLUME_STEPS) {
            player.volume = start + (target - start) * (step / VOLUME_STEPS.toFloat())
            delay(VOLUME_STEP_MS)
            step++
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // SMCPKG_SUPPORT>>>Cursor098
        alarmPlayback?.handle(intent)
        // SMCPKG_SUPPORT<<<Cursor098
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        alarmPlayback?.release()
        spatialRouteJob?.cancel()
        spatialRouteScope.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        // SMCPKG_SUPPORT>>>Cursor013
        // const val USER_AGENT = "Go2RadioLive/1.0.0"
        // const val USER_AGENT = "Go2RadioLive/1.1.0"
        // const val USER_AGENT = "Go2RadioLive/1.1.1"
        // const val USER_AGENT = "Go2RadioLive/1.2.0"
        // const val USER_AGENT = "Go2RadioLive/1.2.1"
        // const val USER_AGENT = "Go2RadioLive/1.3.0"
        // SMCPKG_SUPPORT<<<Cursor013
        // SMCPKG_SUPPORT>>>Cursor087
        // const val USER_AGENT = "Go2RadioLive/1.4.0"
        // SMCPKG_SUPPORT<<<Cursor087
        // SMCPKG_SUPPORT>>>Cursor094
        // const val USER_AGENT = "Go2RadioLive/1.5.0"
        // SMCPKG_SUPPORT<<<Cursor094
        // SMCPKG_SUPPORT>>>Cursor102
        // const val USER_AGENT = "Go2RadioLive/1.5.1"
        // SMCPKG_SUPPORT<<<Cursor102
        // SMCPKG_SUPPORT>>>Cursor106
        // const val USER_AGENT = "Go2RadioLive/1.6.0"
        // SMCPKG_SUPPORT<<<Cursor106
        // SMCPKG_SUPPORT>>>Cursor108
        const val USER_AGENT = "Go2RadioLive/1.6.1"
        // SMCPKG_SUPPORT<<<Cursor108
        const val ACTION_SLEEP_MINUTES = "com.apsmkimo.go2radiolive.SLEEP_MINUTES"
        const val ACTION_SLEEP_EPISODE = "com.apsmkimo.go2radiolive.SLEEP_EPISODE"
        const val ACTION_SLEEP_CANCEL = "com.apsmkimo.go2radiolive.SLEEP_CANCEL"
        const val ACTION_ALARM_PLAY = "com.apsmkimo.go2radiolive.ALARM_PLAY"
        const val ACTION_ALARM_STOP = "com.apsmkimo.go2radiolive.ALARM_STOP"
        const val ACTION_ALARM_SNOOZE = "com.apsmkimo.go2radiolive.ALARM_SNOOZE"
        const val EXTRA_MINUTES = "extra_minutes"
        private const val VOLUME_STEPS = 6
        private const val VOLUME_STEP_MS = 15L
    }
}
