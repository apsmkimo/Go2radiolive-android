package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
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
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.PlaybackState

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
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider()
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.GraphicEq,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = playback.station?.name ?: stringResource(R.string.no_station_playing),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when {
                    playback.isBuffering -> stringResource(R.string.buffering)
                    playback.isPlaying -> stringResource(R.string.now_playing)
                    playback.station != null -> stringResource(R.string.paused)
                    else -> stringResource(R.string.tap_station_to_play)
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (playback.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(22.dp),
                    strokeWidth = 2.dp,
                )
            }
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
    val marginPx = with(LocalDensity.current) { 8.dp.roundToPx() }
    val positionProvider = remember(marginPx) { AboveAnchorPositionProvider(marginPx) }
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
            .heightIn(max = 320.dp)
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
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val x = (anchorBounds.right - popupContentSize.width)
            .coerceIn(marginPx, (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx))
        val y = (anchorBounds.top - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        return IntOffset(x, y)
    }
}
