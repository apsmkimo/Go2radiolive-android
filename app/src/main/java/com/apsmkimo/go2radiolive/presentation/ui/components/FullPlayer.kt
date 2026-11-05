package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
// SMCPKG_SUPPORT>>>Cursor123
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Slider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.delay
// SMCPKG_SUPPORT<<<Cursor123
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.station.stationIconMotion

@Composable
fun FullPlayer(
    playback: PlaybackState,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNextStation: () -> Unit,
    modifier: Modifier = Modifier,
    // SMCPKG_SUPPORT>>>Cursor123
    positionMs: () -> Long = { 0L },
    durationMs: () -> Long = { 0L },
    onSeekTo: (Long) -> Unit = {},
    onSeekBy: (Long) -> Unit = {},
    onOlderEpisode: () -> Unit = {},
    onNewerEpisode: () -> Unit = {},
    // SMCPKG_SUPPORT<<<Cursor123
) {
    val station = playback.station
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            if (station != null) {
                StationArtwork(
                    station = station,
                    motion = stationIconMotion(station.stationUuid, playback),
                    modifier = Modifier.size(220.dp),
                    corner = 28.dp,
                )
            }
            Spacer(Modifier.height(28.dp))
            MarqueeTitle(
                text = station?.name ?: stringResource(R.string.no_station_playing),
                style = MaterialTheme.typography.headlineSmall,
            )
            val status = when {
                playback.cannotConnect -> stringResource(R.string.cannot_connect)
                playback.isBuffering -> stringResource(R.string.buffering)
                playback.isPlaying -> stringResource(R.string.now_playing)
                station != null -> stringResource(R.string.paused)
                else -> stringResource(R.string.tap_station_to_play)
            }
            Text(
                text = status,
                modifier = Modifier.padding(top = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            // SMCPKG_SUPPORT>>>Cursor123
            val isEpisode = station?.directStream == true
            var seekTick by remember { mutableIntStateOf(0) }
            if (isEpisode) {
                EpisodeProgress(
                    stationId = station?.stationUuid.orEmpty(),
                    isPlaying = playback.isPlaying,
                    isBuffering = playback.isBuffering,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    onSeekTo = onSeekTo,
                    refreshKey = seekTick,
                )
                Spacer(Modifier.height(16.dp))
            }
            // SMCPKG_SUPPORT<<<Cursor123
            Row(
                // SMCPKG_SUPPORT>>>Cursor123
                // horizontalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isEpisode) 4.dp else 12.dp),
                // SMCPKG_SUPPORT<<<Cursor123
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // SMCPKG_SUPPORT>>>Cursor123
                if (isEpisode) {
                    IconButton(onClick = onOlderEpisode, enabled = playback.hasOlderEpisode) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = stringResource(R.string.episode_previous),
                        )
                    }
                    IconButton(onClick = { onSeekBy(-SEEK_STEP_MS); seekTick++ }, enabled = !playback.cannotConnect) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = stringResource(R.string.rewind_10),
                        )
                    }
                }
                // SMCPKG_SUPPORT<<<Cursor123
                FilledIconButton(
                    onClick = onTogglePlayPause,
                    enabled = station != null,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.isPlaying) {
                            stringResource(R.string.pause)
                        } else {
                            stringResource(R.string.play)
                        },
                    )
                }
                // SMCPKG_SUPPORT>>>Cursor123
                if (isEpisode) {
                    IconButton(onClick = { onSeekBy(SEEK_STEP_MS); seekTick++ }, enabled = !playback.cannotConnect) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = stringResource(R.string.forward_10),
                        )
                    }
                    IconButton(onClick = onNewerEpisode, enabled = playback.hasNewerEpisode) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = stringResource(R.string.episode_next),
                        )
                    }
                }
                // SMCPKG_SUPPORT<<<Cursor123
            }
            if (playback.cannotConnect) {
                TextButton(onClick = onNextStation, modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = stringResource(R.string.next_station))
                }
            }
        }
    }
}

// SMCPKG_SUPPORT>>>Cursor123
private const val SEEK_STEP_MS = 10_000L
private const val POSITION_POLL_MS = 500L

/**
 * Seek bar with elapsed time on the left and total length on the right.
 * Polls the controller every 500 ms, only while this composable is on screen and playing.
 */
@Composable
private fun EpisodeProgress(
    stationId: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    positionMs: () -> Long,
    durationMs: () -> Long,
    onSeekTo: (Long) -> Unit,
    refreshKey: Int = 0,
) {
    var position by remember(stationId) { mutableLongStateOf(0L) }
    var duration by remember(stationId) { mutableLongStateOf(0L) }
    var dragging by remember(stationId) { mutableStateOf(false) }
    var dragValue by remember(stationId) { mutableFloatStateOf(0f) }
    LaunchedEffect(stationId, isPlaying, isBuffering, refreshKey) {
        position = positionMs()
        duration = durationMs()
        while (isPlaying) {
            delay(POSITION_POLL_MS)
            if (!dragging) position = positionMs()
            duration = durationMs()
        }
    }
    val known = duration > 0L
    val shown = if (dragging) (dragValue * duration).toLong() else position
    val fraction = if (known) (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f
    val seekLabel = stringResource(R.string.seek_position)
    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = if (dragging) dragValue else fraction,
            onValueChange = { value ->
                dragging = true
                dragValue = value
            },
            onValueChangeFinished = {
                val target = (dragValue * duration).toLong()
                dragging = false
                position = target
                onSeekTo(target)
            },
            enabled = known,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = seekLabel },
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatPlaybackTime(shown, duration),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (known) formatPlaybackTime(duration, duration) else "--:--",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** mm:ss, or h:mm:ss once the episode (or the value itself) reaches an hour. */
internal fun formatPlaybackTime(valueMs: Long, totalMs: Long = valueMs): String {
    val totalSeconds = (valueMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    val useHours = hours > 0L || totalMs >= 3_600_000L
    return if (useHours) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)
    }
}
// SMCPKG_SUPPORT<<<Cursor123
