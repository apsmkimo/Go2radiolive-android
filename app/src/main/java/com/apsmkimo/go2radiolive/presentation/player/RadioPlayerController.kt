package com.apsmkimo.go2radiolive.presentation.player

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class RadioPlayerController(
    private val context: Context,
    private val gson: Gson,
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: Pair<RadioStation, String>? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playbackState.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            _playbackState.update { it.copy(isBuffering = buffering) }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val station = mediaItem.toStation()
            if (station != null) {
                _playbackState.update { it.copy(station = station, errorMessage = null) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _playbackState.update {
                it.copy(
                    isPlaying = false,
                    isBuffering = false,
                    errorMessage = error.localizedMessage
                        ?: context.getString(R.string.playback_failed),
                )
            }
        }
    }

    fun initialize() {
        if (controllerFuture != null) return
        val token = SessionToken(
            context,
            ComponentName(context, RadioPlaybackService::class.java),
        )
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController.addListener(listener)
                    syncFromController(mediaController)
                    pendingPlay?.let { (station, url) ->
                        pendingPlay = null
                        play(station, url)
                    }
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun play(station: RadioStation, streamUrl: String) {
        _playbackState.update {
            it.copy(station = station, isBuffering = true, errorMessage = null)
        }
        val extras = Bundle().apply {
            putString(EXTRA_STATION_JSON, gson.toJson(station))
        }
        val artwork = station.faviconUrl.takeIf { it.isNotBlank() }?.toUri()
        val mediaItem = MediaItem.Builder()
            .setMediaId(station.stationUuid)
            .setUri(streamUrl)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(station.name)
                    .setArtist(station.country.ifBlank { context.getString(R.string.app_name) })
                    .setArtworkUri(artwork)
                    .setIsPlayable(true)
                    .setExtras(extras)
                    .build(),
            )
            .build()
        val mediaController = controller
        if (mediaController == null) {
            pendingPlay = station to streamUrl
            initialize()
            return
        }
        mediaController.setMediaItem(mediaItem)
        mediaController.prepare()
        mediaController.play()
    }

    fun togglePlayPause() {
        val mediaController = controller ?: return
        if (mediaController.isPlaying) {
            mediaController.pause()
        } else {
            mediaController.play()
        }
    }

    fun release() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    private fun syncFromController(mediaController: MediaController) {
        val station = mediaController.currentMediaItem.toStation() ?: _playbackState.value.station
        _playbackState.update {
            it.copy(
                station = station,
                isPlaying = mediaController.isPlaying,
                isBuffering = mediaController.playbackState == Player.STATE_BUFFERING,
            )
        }
    }

    private fun MediaItem?.toStation(): RadioStation? {
        val json = this?.mediaMetadata?.extras?.getString(EXTRA_STATION_JSON) ?: return null
        return runCatching { gson.fromJson(json, RadioStation::class.java) }.getOrNull()
    }

    companion object {
        private const val EXTRA_STATION_JSON = "extra_station_json"
    }
}
