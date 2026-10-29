package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.alarm.PlaybackSignals
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.station.stationIconMotion

// SMCPKG_SUPPORT>>>Cursor020
// @Composable
// fun MiniPlayer(
//     playback: PlaybackState,
//     onTogglePlayPause: () -> Unit,
//     modifier: Modifier = Modifier,
// ) {
//     Surface(
//         modifier = modifier.fillMaxWidth(),
//         tonalElevation = 3.dp,
//         color = MaterialTheme.colorScheme.surface,
//     ) {
//         Column {
//             HorizontalDivider()
//             Row(
//                 modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
//                 verticalAlignment = Alignment.CenterVertically,
//             ) {
//                 Icon(
//                     imageVector = Icons.Outlined.GraphicEq,
//                     contentDescription = null,
//                     tint = MaterialTheme.colorScheme.primary,
//                     modifier = Modifier.padding(end = 12.dp),
//                 )
//                 Column(modifier = Modifier.weight(1f)) {
//                     Text(
//                         text = playback.station?.name ?: stringResource(R.string.no_station_playing),
//                         style = MaterialTheme.typography.titleMedium,
//                         maxLines = 1,
//                         overflow = TextOverflow.Ellipsis,
//                     )
//                     val status = when {
//                         playback.isBuffering -> stringResource(R.string.buffering)
//                         playback.isPlaying -> stringResource(R.string.now_playing)
//                         playback.station != null -> stringResource(R.string.paused)
//                         else -> stringResource(R.string.tap_station_to_play)
//                     }
//                     Text(
//                         text = status,
//                         style = MaterialTheme.typography.bodyMedium,
//                         color = MaterialTheme.colorScheme.onSurfaceVariant,
//                     )
//                 }
//                 if (playback.isBuffering) {
//                     CircularProgressIndicator(
//                         modifier = Modifier
//                             .padding(end = 8.dp)
//                             .size(22.dp),
//                         strokeWidth = 2.dp,
//                     )
//                 }
//                 FilledTonalIconButton(
//                     onClick = onTogglePlayPause,
//                     enabled = playback.station != null,
//                 ) {
//                     Icon(
//                         imageVector = if (playback.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
//                         contentDescription = if (playback.isPlaying) {
//                             stringResource(R.string.pause)
//                         } else {
//                             stringResource(R.string.play)
//                         },
//                     )
//                 }
//             }
//         }
//     }
// }
// SMCPKG_SUPPORT<<<Cursor020

@Composable
fun MiniPlayer(
    playback: PlaybackState,
    audioSettings: AudioSettings,
    audioPanelExpanded: Boolean,
    onTogglePlayPause: () -> Unit,
    onAudioPanelExpandedChange: (Boolean) -> Unit,
    onBassChange: (Float) -> Unit,
    onLowChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onHighChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onResetTone: () -> Unit,
    modifier: Modifier = Modifier,
    // SMCPKG_SUPPORT>>>Cursor119
    onOpenPlayer: () -> Unit = {},
    onNextStation: () -> Unit = {},
    // SMCPKG_SUPPORT<<<Cursor119
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // SMCPKG_SUPPORT>>>Cursor119
            // Icon(
            //     imageVector = Icons.Outlined.GraphicEq,
            //     contentDescription = null,
            //     tint = MaterialTheme.colorScheme.primary,
            //     modifier = Modifier.padding(end = 12.dp),
            // )
            val station = playback.station
            if (station == null) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 12.dp),
                )
            } else {
                StationArtwork(
                    station = station,
                    motion = stationIconMotion(station.stationUuid, playback),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(48.dp)
                        .clickable(onClick = onOpenPlayer),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = station != null, onClick = onOpenPlayer),
            ) {
                // Text(
                //     text = playback.station?.name ?: stringResource(R.string.no_station_playing),
                //     style = MaterialTheme.typography.titleMedium,
                //     maxLines = 1,
                //     overflow = TextOverflow.Ellipsis,
                // )
                MarqueeTitle(
                    text = station?.name ?: stringResource(R.string.no_station_playing),
                    style = MaterialTheme.typography.titleMedium,
                )
                val status = when {
                    playback.cannotConnect -> stringResource(R.string.cannot_connect)
                    playback.isBuffering -> stringResource(R.string.buffering)
                    playback.isPlaying -> stringResource(R.string.now_playing)
                    station != null -> stringResource(R.string.paused)
                    else -> stringResource(R.string.tap_station_to_play)
                }
            // SMCPKG_SUPPORT<<<Cursor119
                val sleep by PlaybackSignals.sleep.collectAsStateWithLifecycle()
                val sleepLabel = sleepRemainingLabel(sleep)
                Text(
                    text = if (sleepLabel == null) status else "$status · $sleepLabel",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // SMCPKG_SUPPORT>>>Cursor119
            // if (playback.isBuffering) {
            //     CircularProgressIndicator(
            //         modifier = Modifier
            //             .padding(end = 8.dp)
            //             .size(22.dp),
            //         strokeWidth = 2.dp,
            //     )
            // }
            if (playback.cannotConnect) {
                TextButton(onClick = onNextStation) {
                    Text(
                        text = stringResource(R.string.next_station),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // SMCPKG_SUPPORT<<<Cursor119
            FilledTonalIconButton(
                onClick = onTogglePlayPause,
                enabled = playback.station != null,
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
            if (audioSettings.showPanelButton) {
                AudioPanelButton(
                    expanded = audioPanelExpanded,
                    settings = audioSettings,
                    onExpandedChange = onAudioPanelExpandedChange,
                    onBassChange = onBassChange,
                    onLowChange = onLowChange,
                    onMidChange = onMidChange,
                    onHighChange = onHighChange,
                    onBalanceChange = onBalanceChange,
                    onResetTone = onResetTone,
                )
            }
        }
    }
}

@Composable
private fun AudioPanelButton(
    expanded: Boolean,
    settings: AudioSettings,
    onExpandedChange: (Boolean) -> Unit,
    onBassChange: (Float) -> Unit,
    onLowChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onHighChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onResetTone: () -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val configuration = LocalConfiguration.current
    val safe = WindowInsets.safeDrawing
    val marginPx = with(density) { 8.dp.roundToPx() }
    val topInsetPx = safe.getTop(density)
    val bottomInsetPx = safe.getBottom(density)
    val startInsetPx = safe.getLeft(density, layoutDirection)
    val endInsetPx = safe.getRight(density, layoutDirection)
    val positionProvider = remember(marginPx, topInsetPx, startInsetPx, endInsetPx) {
        AboveAnchorPositionProvider(marginPx, topInsetPx, startInsetPx, endInsetPx)
    }
    val maxPanelHeight = with(density) {
        val screenPx = configuration.screenHeightDp.dp.toPx()
        (screenPx - topInsetPx - bottomInsetPx - 160.dp.toPx()).toDp().coerceIn(180.dp, 440.dp)
    }
    Box(modifier = Modifier.padding(start = 4.dp)) {
        if (expanded) {
            FilledIconButton(onClick = { onExpandedChange(false) }) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.close_audio_panel),
                )
            }
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { onExpandedChange(false) },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
            ) {
                AudioEffectPanel(
                    maxHeight = maxPanelHeight,
                    settings = settings,
                    onBassChange = onBassChange,
                    onLowChange = onLowChange,
                    onMidChange = onMidChange,
                    onHighChange = onHighChange,
                    onBalanceChange = onBalanceChange,
                    onResetTone = onResetTone,
                )
            }
        } else {
            FilledTonalIconButton(onClick = { onExpandedChange(true) }) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = stringResource(R.string.open_audio_panel),
                )
            }
        }
    }
}

@Composable
private fun AudioEffectPanel(
    maxHeight: androidx.compose.ui.unit.Dp,
    settings: AudioSettings,
    onBassChange: (Float) -> Unit,
    onLowChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onHighChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onResetTone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .widthIn(min = 260.dp, max = 340.dp)
            // SMCPKG_SUPPORT>>>Cursor109
            // .heightIn(max = 440.dp)
            // SMCPKG_SUPPORT<<<Cursor109
            // SMCPKG_SUPPORT>>>Cursor112
            .heightIn(max = maxHeight)
            // SMCPKG_SUPPORT<<<Cursor112
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.audio_panel),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        AudioEffectControls(
            settings = settings,
            onBassChange = onBassChange,
            onLowChange = onLowChange,
            onMidChange = onMidChange,
            onHighChange = onHighChange,
            onBalanceChange = onBalanceChange,
            onReset = onResetTone,
        )
    }
}

private class AboveAnchorPositionProvider(
    private val marginPx: Int,
    private val topInsetPx: Int,
    private val startInsetPx: Int,
    private val endInsetPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val minX = startInsetPx + marginPx
        val maxX = (windowSize.width - endInsetPx - popupContentSize.width - marginPx).coerceAtLeast(minX)
        val preferredX = if (layoutDirection == LayoutDirection.Rtl) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = preferredX.coerceIn(minX, maxX)
        val minY = topInsetPx + marginPx
        val y = (anchorBounds.top - popupContentSize.height - marginPx).coerceAtLeast(minY)
        return IntOffset(x, y)
    }
}
