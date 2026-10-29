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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
            }
            if (playback.cannotConnect) {
                TextButton(onClick = onNextStation, modifier = Modifier.padding(top = 12.dp)) {
                    Text(text = stringResource(R.string.next_station))
                }
            }
        }
    }
}
