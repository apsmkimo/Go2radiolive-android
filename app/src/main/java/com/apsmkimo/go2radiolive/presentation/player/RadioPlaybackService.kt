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
import com.apsmkimo.go2radiolive.presentation.MainActivity

@OptIn(UnstableApi::class)
class RadioPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

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
        const val USER_AGENT = "Go2RadioLive/1.2.0"
        // SMCPKG_SUPPORT<<<Cursor013
    }
}
