package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory

@Composable
fun StationList(
    stations: List<RadioStation>,
    favoriteIds: Set<String>,
    isLoading: Boolean,
    errorMessage: String?,
    emptyText: String,
    onStationClick: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && stations.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null && stations.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onRetry) {
                    Text(text = errorMessage + "\n" + stringResource(R.string.retry), textAlign = TextAlign.Center)
                }
            }
        }
        stations.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(stations, key = { it.stationUuid }) { station ->
                    StationCard(
                        station = station,
                        isFavorite = station.stationUuid in favoriteIds,
                        onClick = { onStationClick(station) },
                        onToggleFavorite = { onToggleFavorite(station) },
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryList(
    categories: List<StationCategory>,
    isLoading: Boolean,
    errorMessage: String?,
    onCategoryClick: (StationCategory) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    BrowseRowList(
        rows = categories.map { category ->
            BrowseRow(
                id = category.name,
                title = category.name,
                subtitle = context.getString(R.string.station_count, category.stationCount),
            )
        },
        isLoading = isLoading,
        errorMessage = errorMessage,
        emptyText = stringResource(R.string.empty_categories),
        onRowClick = { row ->
            categories.firstOrNull { it.name == row.id }?.let(onCategoryClick)
        },
        onRetry = onRetry,
        modifier = modifier,
    )
}

data class BrowseRow(
    val id: String,
    val title: String,
    val subtitle: String,
)

@Composable
fun BrowseRowList(
    rows: List<BrowseRow>,
    isLoading: Boolean,
    errorMessage: String?,
    emptyText: String,
    onRowClick: (BrowseRow) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        isLoading && rows.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null && rows.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextButton(onClick = onRetry) {
                    Text(text = errorMessage + "\n" + stringResource(R.string.retry), textAlign = TextAlign.Center)
                }
            }
        }
        rows.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp),
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    androidx.compose.material3.ElevatedCard(
                        onClick = { onRowClick(row) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            if (row.subtitle.isNotEmpty()) {
                                Text(
                                    text = row.subtitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
