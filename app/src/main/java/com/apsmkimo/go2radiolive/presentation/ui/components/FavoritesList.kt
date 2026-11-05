package com.apsmkimo.go2radiolive.presentation.ui.components

// SMCPKG_SUPPORT>>>Cursor123
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.station.stationIsFavorite

/** Favorites tab: saved stations / episodes first, then saved podcast shows. */
@Composable
fun FavoritesList(
    stations: List<RadioStation>,
    favoriteIds: Set<String>,
    shows: List<PodcastShow>,
    favoriteShowIds: Set<String>,
    isLoading: Boolean,
    onStationClick: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onShowClick: (PodcastShow) -> Unit,
    onToggleFavoriteShow: (PodcastShow) -> Unit,
    modifier: Modifier = Modifier,
    scrollToken: Int = 0,
    playback: PlaybackState = PlaybackState(),
) {
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToken) {
        if (scrollToken > 0) listState.scrollToItem(0)
    }
    if (isLoading && stations.isEmpty() && shows.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "fav-stations-header", contentType = "fav-header") {
            FavoritesSectionTitle(stringResource(R.string.favorites_section_stations))
        }
        if (stations.isEmpty()) {
            item(key = "fav-stations-empty", contentType = "fav-empty") {
                FavoritesEmptyLine(stringResource(R.string.empty_favorites))
            }
        } else {
            items(stations, key = { "st-" + it.stationUuid }, contentType = { "station" }) { station ->
                StationCard(
                    station = station,
                    isFavorite = stationIsFavorite(station, favoriteIds),
                    onClick = { onStationClick(station) },
                    onToggleFavorite = { onToggleFavorite(station) },
                    playback = playback,
                )
            }
        }
        item(key = "fav-shows-header", contentType = "fav-header") {
            FavoritesSectionTitle(
                text = stringResource(R.string.favorites_section_podcasts),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (shows.isEmpty()) {
            item(key = "fav-shows-empty", contentType = "fav-empty") {
                FavoritesEmptyLine(stringResource(R.string.empty_favorite_shows))
            }
        } else {
            items(shows, key = { "show-" + it.id }, contentType = { "show" }) { show ->
                ElevatedCard(
                    onClick = { onShowClick(show) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    ShowRowContent(
                        title = show.name,
                        subtitle = show.author,
                        isFavorite = show.id in favoriteShowIds,
                        onToggleFavorite = { onToggleFavoriteShow(show) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoritesSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

@Composable
private fun FavoritesEmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
    )
}
// SMCPKG_SUPPORT<<<Cursor123
