package com.apsmkimo.go2radiolive.presentation.player

import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.data.local.StationFailureStore
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.station.PlaylistBodySource
import com.apsmkimo.go2radiolive.domain.station.PlaylistParser
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class RadioPlayerController(
    private val context: Context,
    private val gson: Gson,
    // SMCPKG_SUPPORT>>>Cursor119
    private val playlists: PlaylistBodySource,
    private val failures: StationFailureStore,
    private val nowMs: () -> Long = { System.currentTimeMillis() },
    private val elapsedNow: () -> Long = { SystemClock.elapsedRealtime() },
    // SMCPKG_SUPPORT<<<Cursor119
) {
    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingPlay: Pair<RadioStation, String>? = null

    // SMCPKG_SUPPORT>>>Cursor091
    /** True after the MediaController has reported playback at least once. */
    @Volatile
    var playbackObserved: Boolean = false
        private set
    // SMCPKG_SUPPORT<<<Cursor091

    // SMCPKG_SUPPORT>>>Cursor119
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var attemptJob: Job? = null
    private var attemptToken = 0
    private var signal: CompletableDeferred<Boolean>? = null
    private var givingUp = false
    private var arming = false
    private var suppressRetry = false
    private var activeStation: RadioStation? = null
    private var activeEntryUrl: String? = null
    // SMCPKG_SUPPORT<<<Cursor119

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            // SMCPKG_SUPPORT>>>Cursor119
            // _playbackState.update { it.copy(isPlaying = isPlaying, errorMessage = null) }
            _playbackState.update {
                it.copy(
                    isPlaying = isPlaying,
                    errorMessage = null,
                    cannotConnect = if (isPlaying) false else it.cannotConnect,
                )
            }
            if (isPlaying) signal?.takeIf { waiting -> !waiting.isCompleted }?.complete(true)
            // SMCPKG_SUPPORT<<<Cursor119
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val buffering = playbackState == Player.STATE_BUFFERING
            // SMCPKG_SUPPORT>>>Cursor119
            // _playbackState.update { it.copy(isBuffering = buffering) }
            _playbackState.update {
                it.copy(
                    isBuffering = buffering,
                    cannotConnect = if (playbackState == Player.STATE_READY) false else it.cannotConnect,
                )
            }
            if (playbackState == Player.STATE_READY) {
                signal?.takeIf { waiting -> !waiting.isCompleted }?.complete(true)
            }
            // SMCPKG_SUPPORT<<<Cursor119
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val station = mediaItem.toStation()
            if (station != null) {
                _playbackState.update { it.copy(station = station, errorMessage = null) }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.w(TAG, "playback failed", error)
            // SMCPKG_SUPPORT>>>Cursor119
            // _playbackState.update {
            //     it.copy(
            //         isPlaying = false,
            //         isBuffering = false,
            //         errorMessage = context.getString(R.string.playback_failed),
            //     )
            // }
            if (arming || givingUp || suppressRetry) return
            val waiting = signal
            if (waiting != null && !waiting.isCompleted) {
                waiting.complete(false)
                return
            }
            if (givingUp || attemptJob?.isActive == true) return
            val station = activeStation ?: _playbackState.value.station ?: return
            val entry = activeEntryUrl ?: station.streamUrl
            startAttempts(station, entry)
            // SMCPKG_SUPPORT<<<Cursor119
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
                    // SMCPKG_SUPPORT>>>Cursor091
                    playbackObserved = true
                    // SMCPKG_SUPPORT<<<Cursor091
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
        // SMCPKG_SUPPORT>>>Cursor098
        (context.applicationContext as? Go2RadioApp)?.container?.wakeAlarmRepository?.remember(station)
        // SMCPKG_SUPPORT<<<Cursor098
        // SMCPKG_SUPPORT>>>Cursor119
        // _playbackState.update {
        //     it.copy(station = station, isBuffering = true, errorMessage = null)
        // }
        // val mediaItem = stationMediaItem(context, gson, station, streamUrl)
        // val mediaController = controller
        // if (mediaController == null) {
        //     pendingPlay = station to streamUrl
        //     initialize()
        //     return
        // }
        // mediaController.setMediaItem(mediaItem)
        // mediaController.prepare()
        // mediaController.play()
        startAttempts(station, streamUrl)
        // SMCPKG_SUPPORT<<<Cursor119
    }

    // SMCPKG_SUPPORT>>>Cursor119
    private fun startAttempts(station: RadioStation, entryUrl: String) {
        suppressRetry = false
        val token = ++attemptToken
        attemptJob?.cancel()
        signal = null
        activeStation = station
        activeEntryUrl = entryUrl
        _playbackState.update {
            it.copy(
                station = station,
                isBuffering = true,
                isPlaying = false,
                cannotConnect = false,
                errorMessage = null,
            )
        }
        attemptJob = scope.launch {
            try {
                runAttempts(token, station, entryUrl)
            } catch (cancelled: CancellationException) {
                throw cancelled
            }
        }
    }

    private suspend fun runAttempts(token: Int, station: RadioStation, entryUrl: String) {
        val deadline = elapsedNow() + CONNECT_BUDGET_MS
        val queue = ArrayDeque(PlaylistParser.playbackCandidates(station, entryUrl))
        val tried = HashSet<String>()
        while (token == attemptToken && queue.isNotEmpty() && elapsedNow() < deadline) {
            val url = queue.removeFirst().trim()
            if (url.isEmpty() || !tried.add(url)) continue
            if (PlaylistParser.isPlaylistUrl(url)) {
                val nested = fetchPlaylist(url, deadline)
                nested.asReversed().forEach { queue.addFirst(it) }
                continue
            }
            if (!PlaylistParser.isHttp(url)) continue
            if (!awaitController(deadline) || token != attemptToken) return
            val deferred = CompletableDeferred<Boolean>()
            if (!playOnPlayer(station, url, deferred)) continue
            val remaining = deadline - elapsedNow()
            if (remaining <= 0L) break
            val connected = withTimeoutOrNull(remaining) { deferred.await() } == true
            if (connected && token == attemptToken) {
                signal = null
                failures.recordSuccess(station.stationUuid, nowMs())
                _playbackState.update { it.copy(cannotConnect = false, errorMessage = null) }
                return
            }
        }
        if (token != attemptToken) return
        giveUp(token, station)
    }

    private suspend fun fetchPlaylist(url: String, deadline: Long): List<String> {
        val remaining = deadline - elapsedNow()
        if (remaining <= 0L) return emptyList()
        val budget = remaining.coerceAtMost(PLAYLIST_FETCH_MS)
        if (budget < 500L) return emptyList()
        val body = withContext(Dispatchers.IO) {
            playlists.fetch(url, budget)
        }
        if (body.isNullOrBlank()) return emptyList()
        return PlaylistParser.parse(body, url)
    }

    private suspend fun awaitController(deadline: Long): Boolean {
        if (controller != null) return true
        initialize()
        while (controller == null && elapsedNow() < deadline) {
            delay(50)
        }
        return controller != null
    }

    private fun playOnPlayer(
        station: RadioStation,
        url: String,
        deferred: CompletableDeferred<Boolean>,
    ): Boolean {
        val mediaController = controller ?: return false
        val mediaItem = stationMediaItem(context, gson, station, url)
        return runCatching {
            signal = null
            arming = true
            try {
                mediaController.stop()
            } finally {
                arming = false
            }
            signal = deferred
            mediaController.setMediaItem(mediaItem)
            mediaController.prepare()
            mediaController.play()
            true
        }.getOrDefault(false)
    }

    private suspend fun giveUp(token: Int, station: RadioStation) {
        if (token != attemptToken) return
        givingUp = true
        suppressRetry = true
        signal = null
        runCatching {
            controller?.pause()
            controller?.stop()
            controller?.clearMediaItems()
        }
        givingUp = false
        if (token != attemptToken) return
        failures.recordFailure(station.stationUuid, nowMs())
        if (token != attemptToken) return
        _playbackState.update {
            it.copy(
                station = station,
                isPlaying = false,
                isBuffering = false,
                cannotConnect = true,
                errorMessage = null,
            )
        }
    }
    // SMCPKG_SUPPORT<<<Cursor119

    fun togglePlayPause() {
        val mediaController = controller
        val failed = _playbackState.value.cannotConnect
        if (mediaController != null && mediaController.mediaItemCount > 0 && !failed) {
            if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
            return
        }
        val station = activeStation ?: _playbackState.value.station ?: return
        play(station, activeEntryUrl ?: station.streamUrl)
    }

    fun release() {
        // SMCPKG_SUPPORT>>>Cursor119
        attemptToken += 1
        attemptJob?.cancel()
        // SMCPKG_SUPPORT<<<Cursor119
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
        private const val TAG = "RadioPlayer"
        private const val EXTRA_STATION_JSON = "extra_station_json"
        // SMCPKG_SUPPORT>>>Cursor119
        const val CONNECT_BUDGET_MS = 10_000L
        private const val PLAYLIST_FETCH_MS = 5_000L
        // SMCPKG_SUPPORT<<<Cursor119
    }
}
