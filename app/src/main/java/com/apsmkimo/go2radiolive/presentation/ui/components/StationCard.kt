package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apsmkimo.go2radiolive.data.remote.plainPodcastText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.RadioStation

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
            verticalAlignment = if (episodeNotes) Alignment.Top else Alignment.CenterVertically,
            // SMCPKG_SUPPORT<<<Cursor117
        ) {
            val context = LocalContext.current
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(station.faviconUrl.takeIf { it.isNotBlank() })
                    .crossfade(true)
                    .build(),
                contentDescription = station.name,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
                error = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.primaryContainer,
                ),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.primaryContainer,
                ),
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(
                    MaterialTheme.colorScheme.primaryContainer,
                ),
            )
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
                        Text(
                            text = notes,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                } else {
                // SMCPKG_SUPPORT<<<Cursor117
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
