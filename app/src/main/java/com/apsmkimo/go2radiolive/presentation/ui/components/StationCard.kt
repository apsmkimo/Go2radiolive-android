package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apsmkimo.go2radiolive.data.remote.plainPodcastText
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.station.stationIconMotion

@Composable
fun StationCard(
    station: RadioStation,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    // SMCPKG_SUPPORT>>>Cursor117
    episodeNotes: Boolean = false,
    // SMCPKG_SUPPORT<<<Cursor117
    // SMCPKG_SUPPORT>>>Cursor118
    notesExpanded: Boolean = false,
    notesOverflow: Boolean = false,
    onToggleNotes: () -> Unit = {},
    onNotesOverflow: () -> Unit = {},
    // SMCPKG_SUPPORT<<<Cursor118
    // SMCPKG_SUPPORT>>>Cursor119
    playback: PlaybackState = PlaybackState(),
    // SMCPKG_SUPPORT<<<Cursor119
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            // SMCPKG_SUPPORT>>>Cursor117
            // verticalAlignment = Alignment.CenterVertically,
            // SMCPKG_SUPPORT>>>Cursor119
            // verticalAlignment = if (episodeNotes) Alignment.Top else Alignment.CenterVertically,
            verticalAlignment = Alignment.Top,
            // SMCPKG_SUPPORT<<<Cursor119
            // SMCPKG_SUPPORT<<<Cursor117
        ) {
            // SMCPKG_SUPPORT>>>Cursor119
            // val context = LocalContext.current
            // AsyncImage(
            //     model = ImageRequest.Builder(context)
            //         .data(station.faviconUrl.takeIf { it.isNotBlank() })
            //         .crossfade(true)
            //         .build(),
            //     contentDescription = station.name,
            //     modifier = Modifier
            //         .size(52.dp)
            //         .clip(RoundedCornerShape(12.dp)),
            //     contentScale = ContentScale.Crop,
            //     error = androidx.compose.ui.graphics.painter.ColorPainter(
            //         MaterialTheme.colorScheme.primaryContainer,
            //     ),
            //     fallback = androidx.compose.ui.graphics.painter.ColorPainter(
            //         MaterialTheme.colorScheme.primaryContainer,
            //     ),
            //     placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
            //         MaterialTheme.colorScheme.primaryContainer,
            //     ),
            // )
            StationArtwork(
                station = station,
                motion = stationIconMotion(station.stationUuid, playback),
                modifier = Modifier.size(52.dp),
            )
            // SMCPKG_SUPPORT<<<Cursor119
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            ) {
                // SMCPKG_SUPPORT>>>Cursor117
                if (episodeNotes) {
                    Text(
                        text = plainPodcastText(station.name).ifBlank { station.name },
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    val notes = plainPodcastText(station.summary.orEmpty())
                    if (notes.isNotEmpty()) {
                        // SMCPKG_SUPPORT>>>Cursor118
                        EpisodeDescription(
                            episodeId = station.stationUuid,
                            notes = notes,
                            expanded = notesExpanded,
                            knownOverflow = notesOverflow,
                            onToggle = onToggleNotes,
                            onOverflow = onNotesOverflow,
                        )
                        // SMCPKG_SUPPORT<<<Cursor118
                    }
                } else {
                // SMCPKG_SUPPORT<<<Cursor117
                // SMCPKG_SUPPORT>>>Cursor119
                // Text(
                //     text = station.name,
                //     style = MaterialTheme.typography.titleMedium,
                //     maxLines = 1,
                //     overflow = TextOverflow.Ellipsis,
                // )
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                // SMCPKG_SUPPORT<<<Cursor119
                val subtitle = listOf(station.country, station.tags)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                    .ifBlank { stringResource(R.string.online_radio) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Radio,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // SMCPKG_SUPPORT>>>Cursor117
                }
                // SMCPKG_SUPPORT<<<Cursor117
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = stringResource(R.string.favorite),
                    tint = if (isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}

// SMCPKG_SUPPORT>>>Cursor118
@Composable
private fun EpisodeDescription(
    episodeId: String,
    notes: String,
    expanded: Boolean,
    knownOverflow: Boolean,
    onToggle: () -> Unit,
    onOverflow: () -> Unit,
) {
    var overflows by remember(episodeId, notes) { mutableStateOf(knownOverflow) }
    var reportOverflow by remember(episodeId, notes) { mutableStateOf(false) }
    val seeMore = stringResource(R.string.episode_see_more)
    val showLess = stringResource(R.string.episode_show_less)
    val toggleLabel = if (expanded) showLess else seeMore
    // Arm animation only after the collapsed measurement (and its control) has
    // been placed, so scrolling a row into view does not grow the list.
    var placed by remember(episodeId, notes) { mutableStateOf(false) }
    var animate by remember(episodeId, notes) { mutableStateOf(false) }
    val interaction = remember(episodeId) { MutableInteractionSource() }
    if (placed && !animate) {
        SideEffect { animate = true }
    }
    if (reportOverflow) {
        SideEffect {
            onOverflow()
            reportOverflow = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .then(
                if (animate) {
                    Modifier.animateContentSize(
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Text(
            text = notes,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = episodeDescriptionMaxLines(expanded),
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            onTextLayout = { layout ->
                if (!expanded && layout.hasVisualOverflow) {
                    overflows = true
                    reportOverflow = true
                }
                placed = true
            },
        )
        if (showEpisodeDescriptionToggle(overflows, expanded)) {
            Text(
                text = toggleLabel,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.primary,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 2.dp)
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onToggle,
                    )
                    .padding(vertical = 8.dp, horizontal = 4.dp)
                    .semantics { contentDescription = toggleLabel },
            )
        }
    }
}
// SMCPKG_SUPPORT<<<Cursor118
