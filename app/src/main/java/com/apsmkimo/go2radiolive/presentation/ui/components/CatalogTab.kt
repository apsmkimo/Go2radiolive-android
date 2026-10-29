package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.presentation.ui.safeDrawingPadding
import com.apsmkimo.go2radiolive.ads.AdsEntitlement
import com.apsmkimo.go2radiolive.ads.nativeAdInsertAfterIndex
import com.apsmkimo.go2radiolive.data.remote.CatalogTopics
import com.apsmkimo.go2radiolive.domain.model.CatalogLanguage
import com.apsmkimo.go2radiolive.domain.model.CatalogLevel
import com.apsmkimo.go2radiolive.domain.model.CatalogSort
import com.apsmkimo.go2radiolive.domain.model.CategoryHub
import com.apsmkimo.go2radiolive.domain.model.CountResult
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.station.stationIsFavorite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogTab(
    scope: String,
    level: CatalogLevel,
    groupId: String?,
    listKind: String?,
    hub: CategoryHub?,
    stations: List<RadioStation>,
    others: List<RadioStation>,
    countries: List<CountryOption>,
    sort: CatalogSort,
    languageFilter: String,
    highBitrate: Boolean,
    query: String,
    isLoading: Boolean,
    errorMessage: String?,
    favoriteIds: Set<String>,
    scrollToken: Int,
    onScope: (String) -> Unit,
    onOpenGroup: (String, String) -> Unit,
    onOpenTopic: (String, String) -> Unit,
    onOpenLanguage: (String) -> Unit,
    onOpenRank: (String, String) -> Unit,
    onSort: (CatalogSort) -> Unit,
    onLanguageFilter: (String) -> Unit,
    onHighBitrate: (Boolean) -> Unit,
    onStation: (RadioStation) -> Unit,
    onFavorite: (RadioStation) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    // SMCPKG_SUPPORT>>>Cursor119
    playback: PlaybackState = PlaybackState(),
    // SMCPKG_SUPPORT<<<Cursor119
) {
    var scopeOpen by remember { mutableStateOf(false) }
    var languageOpen by remember { mutableStateOf(false) }
    val labels = catalogLabels()
    val scopeLabel = if (scope == CatalogTopics.GLOBAL) {
        stringResource(R.string.catalog_global)
    } else {
        countries.firstOrNull { it.code.equals(scope, ignoreCase = true) }?.name ?: scope
    }
    Column(modifier.fillMaxSize()) {
        OutlinedButton(
            onClick = { scopeOpen = true },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(scopeLabel, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        val filteredQuery = query.trim()
        val hasRows = when (level) {
            CatalogLevel.LIST -> stations.isNotEmpty() || others.isNotEmpty()
            CatalogLevel.GROUP -> hub != null
            CatalogLevel.HUB -> hub != null && hub.hasContent()
        }
        when {
            isLoading && !hasRows -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && !hasRows && !isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onRetry) {
                        Text(
                            text = errorMessage + "\n" + stringResource(R.string.retry),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            level == CatalogLevel.LIST -> {
                CatalogStationPage(
                    kind = listKind.orEmpty(),
                    stations = stations,
                    others = others,
                    languages = hub?.languages.orEmpty(),
                    sort = sort,
                    languageFilter = languageFilter,
                    highBitrate = highBitrate,
                    query = filteredQuery,
                    favoriteIds = favoriteIds,
                    scrollToken = scrollToken,
                    onSort = onSort,
                    onLanguage = { languageOpen = true },
                    onHighBitrate = onHighBitrate,
                    onStation = onStation,
                    onFavorite = onFavorite,
                    // SMCPKG_SUPPORT>>>Cursor119
                    playback = playback,
                    // SMCPKG_SUPPORT<<<Cursor119
                )
            }
            level == CatalogLevel.GROUP && groupId == CatalogTopics.LANGUAGE -> {
                LanguageList(
                    languages = hub?.languages.orEmpty().filter {
                        filteredQuery.isEmpty() || it.name.contains(filteredQuery, ignoreCase = true)
                    },
                    onOpen = onOpenLanguage,
                )
            }
            level == CatalogLevel.GROUP -> {
                TopicGrid(
                    ids = CatalogTopics.children(groupId.orEmpty()),
                    labels = labels,
                    counts = hub?.counts.orEmpty(),
                    query = filteredQuery,
                    onOpen = onOpenTopic,
                )
            }
            else -> {
                CatalogHubBody(
                    hub = hub,
                    labels = labels,
                    query = filteredQuery,
                    onOpenGroup = onOpenGroup,
                    onOpenTopic = onOpenTopic,
                    onOpenRank = onOpenRank,
                    onOpenLanguage = onOpenLanguage,
                )
            }
        }
    }
    if (scopeOpen) {
        CatalogScopeDialog(
            selected = scope,
            countries = countries,
            onDismiss = { scopeOpen = false },
            onSelect = {
                scopeOpen = false
                onScope(it)
            },
        )
    }
    if (languageOpen) {
        CatalogLanguageDialog(
            languages = hub?.languages.orEmpty(),
            selected = languageFilter,
            onDismiss = { languageOpen = false },
            onSelect = {
                languageOpen = false
                onLanguageFilter(it)
            },
        )
    }
}

@Composable
private fun CatalogHubBody(
    hub: CategoryHub?,
    labels: Map<String, String>,
    query: String,
    onOpenGroup: (String, String) -> Unit,
    onOpenTopic: (String, String) -> Unit,
    onOpenRank: (String, String) -> Unit,
    onOpenLanguage: (String) -> Unit,
) {
    val counts = hub?.counts.orEmpty()
    val cards = CatalogTopics.hubCards.map { id ->
        id to (labels[id] ?: id)
    }.filter { (_, label) -> query.isEmpty() || label.contains(query, ignoreCase = true) }
    val ranks = listOf(
        CatalogTopics.RANK_VOTES to stringResource(R.string.catalog_rank_votes),
        CatalogTopics.RANK_CLICKS to stringResource(R.string.catalog_rank_clicks),
        CatalogTopics.RANK_TREND to stringResource(R.string.catalog_rank_trend),
    ).filter { (_, label) -> query.isEmpty() || label.contains(query, ignoreCase = true) }
    val languages = hub?.languages.orEmpty().filter {
        query.isEmpty() || it.name.contains(query, ignoreCase = true)
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ranks.isNotEmpty()) {
            item(key = "rank-title") { SectionTitle(stringResource(R.string.catalog_rankings)) }
            items(ranks, key = { it.first }) { (kind, label) ->
                val stations = when (kind) {
                    CatalogTopics.RANK_VOTES -> hub?.votes
                    CatalogTopics.RANK_TREND -> hub?.trend
                    else -> hub?.clicks
                }
                CatalogCard(
                    title = label,
                    subtitle = if (stations == null) "" else stringResource(R.string.station_count, stations.size),
                    onClick = { onOpenRank(kind, label) },
                )
            }
        }
        if (cards.isNotEmpty()) {
            item(key = "cat-title") { SectionTitle(stringResource(R.string.catalog_section)) }
        }
        items(cards.chunked(2), key = { row -> row.joinToString { it.first } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, label) ->
                    val count = cardCount(id, counts, hub?.languages)
                    CatalogCard(
                        title = label,
                        subtitle = countLabel(count),
                        onClick = {
                            if (CatalogTopics.isGroup(id)) onOpenGroup(id, label) else onOpenTopic(id, label)
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (query.isNotEmpty() && languages.isNotEmpty()) {
            item(key = "lang-title") { SectionTitle(stringResource(R.string.catalog_languages)) }
            items(languages, key = { "q-${it.name}" }) { language ->
                CatalogCard(
                    title = language.name,
                    subtitle = stringResource(R.string.station_count, language.stationCount),
                    onClick = { onOpenLanguage(language.name) },
                )
            }
        }
    }
}

@Composable
private fun TopicGrid(
    ids: List<String>,
    labels: Map<String, String>,
    counts: Map<String, CountResult>,
    query: String,
    onOpen: (String, String) -> Unit,
) {
    val rows = ids.map { id -> id to (labels[id] ?: id) }
        .filter { (_, label) -> query.isEmpty() || label.contains(query, ignoreCase = true) }
    if (rows.isEmpty()) {
        EmptyMessage(stringResource(R.string.empty_categories))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(rows.chunked(2), key = { row -> row.joinToString { it.first } }) { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (id, label) ->
                    CatalogCard(
                        title = label,
                        subtitle = countLabel(counts[id]),
                        onClick = { onOpen(id, label) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LanguageList(
    languages: List<CatalogLanguage>,
    onOpen: (String) -> Unit,
) {
    if (languages.isEmpty()) {
        EmptyMessage(stringResource(R.string.empty_categories))
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(languages, key = { it.name }) { language ->
            CatalogCard(
                title = language.name,
                subtitle = stringResource(R.string.station_count, language.stationCount),
                onClick = { onOpen(language.name) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogStationPage(
    kind: String,
    stations: List<RadioStation>,
    others: List<RadioStation>,
    languages: List<CatalogLanguage>,
    sort: CatalogSort,
    languageFilter: String,
    highBitrate: Boolean,
    query: String,
    favoriteIds: Set<String>,
    scrollToken: Int,
    onSort: (CatalogSort) -> Unit,
    onLanguage: () -> Unit,
    onHighBitrate: (Boolean) -> Unit,
    onStation: (RadioStation) -> Unit,
    onFavorite: (RadioStation) -> Unit,
    // SMCPKG_SUPPORT>>>Cursor119
    playback: PlaybackState = PlaybackState(),
    // SMCPKG_SUPPORT<<<Cursor119
) {
    // SMCPKG_SUPPORT>>>Cursor113
    val adsRemoved by AdsEntitlement.removed.collectAsStateWithLifecycle()
    // SMCPKG_SUPPORT<<<Cursor113
    val ranking = kind.startsWith("rank:")
    val languagePage = kind.startsWith("lang:")
    val visible = stations.filter { matchesQuery(it, query) }
    val visibleOthers = others.filter { matchesQuery(it, query) }
    val rows = buildList {
        visible.forEachIndexed { index, station -> add(CatalogRow.StationRow(station, index)) }
        if (visibleOthers.isNotEmpty()) {
            add(CatalogRow.OtherHeader)
            visibleOthers.forEachIndexed { index, station ->
                add(CatalogRow.StationRow(station, visible.size + index))
            }
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(scrollToken) {
        if (scrollToken > 0 && (visible.isNotEmpty() || visibleOthers.isNotEmpty())) {
            listState.scrollToItem(0)
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!ranking) {
                FilterChip(
                    selected = sort == CatalogSort.CLICKS,
                    onClick = { onSort(CatalogSort.CLICKS) },
                    label = { Text(stringResource(R.string.catalog_sort_clicks)) },
                )
                FilterChip(
                    selected = sort == CatalogSort.VOTES,
                    onClick = { onSort(CatalogSort.VOTES) },
                    label = { Text(stringResource(R.string.catalog_sort_votes)) },
                )
                FilterChip(
                    selected = sort == CatalogSort.NAME,
                    onClick = { onSort(CatalogSort.NAME) },
                    label = { Text(stringResource(R.string.catalog_sort_name)) },
                )
            }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = !highBitrate,
                onClick = { onHighBitrate(false) },
                label = { Text(stringResource(R.string.catalog_quality_all)) },
            )
            FilterChip(
                selected = highBitrate,
                onClick = { onHighBitrate(true) },
                label = { Text(stringResource(R.string.catalog_quality_high)) },
            )
            if (!languagePage && languages.isNotEmpty()) {
                val languageLabel = languageFilter.ifBlank { stringResource(R.string.catalog_language_all) }
                FilterChip(
                    selected = languageFilter.isNotBlank(),
                    onClick = onLanguage,
                    label = { Text(languageLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                )
            }
        }
        if (visible.isEmpty() && visibleOthers.isEmpty()) {
            EmptyMessage(stringResource(R.string.empty_stations))
            return
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(rows, key = { row ->
                when (row) {
                    CatalogRow.OtherHeader -> "other-header"
                    is CatalogRow.StationRow -> row.station.stationUuid + "-" + row.index
                }
            }) { row ->
                when (row) {
                    CatalogRow.OtherHeader -> {
                        Text(
                            text = stringResource(R.string.catalog_other_countries),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    is CatalogRow.StationRow -> {
                        StationCard(
                            station = row.station,
                            // SMCPKG_SUPPORT>>>Cursor119
                            // isFavorite = row.station.stationUuid in favoriteIds,
                            isFavorite = stationIsFavorite(row.station, favoriteIds),
                            playback = playback,
                            // SMCPKG_SUPPORT<<<Cursor119
                            onClick = { onStation(row.station) },
                            onToggleFavorite = { onFavorite(row.station) },
                        )
                        // SMCPKG_SUPPORT>>>Cursor113
                        // if (nativeAdInsertAfterIndex(row.index)) NativeAdSlot()
                        if (!adsRemoved && nativeAdInsertAfterIndex(row.index)) NativeAdSlot()
                        // SMCPKG_SUPPORT<<<Cursor113
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.ElevatedCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(88.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.Center) {
            Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

@Composable
private fun EmptyMessage(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(24.dp),
        )
    }
}

@Composable
private fun CatalogScopeDialog(
    selected: String,
    countries: List<CountryOption>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val global = stringResource(R.string.catalog_global)
    val showGlobal = trimmed.isEmpty() || global.contains(trimmed, ignoreCase = true)
    val rows = countries.filter {
        trimmed.isEmpty() ||
            it.name.contains(trimmed, ignoreCase = true) ||
            it.code.contains(trimmed, ignoreCase = true)
    }.sortedBy { it.name.lowercase() }
    val listHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp.coerceAtMost(420.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        // SMCPKG_SUPPORT>>>Cursor112
        modifier = Modifier.padding(safeDrawingPadding()),
        // SMCPKG_SUPPORT<<<Cursor112
        title = { Text(stringResource(R.string.catalog_scope)) },
        text = {
            Column(Modifier.heightIn(max = listHeight).fillMaxWidth()) {
                androidx.compose.material3.OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.catalog_scope_search)) },
                )
                LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                    if (showGlobal) {
                        item(key = "global") {
                            TextButton(onClick = { onSelect(CatalogTopics.GLOBAL) }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = global + if (selected == CatalogTopics.GLOBAL) "  ✓" else "",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                    items(rows, key = { it.code }) { country ->
                        TextButton(onClick = { onSelect(country.code) }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = country.name + if (country.code.equals(selected, true)) "  ✓" else "",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun CatalogLanguageDialog(
    languages: List<CatalogLanguage>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val listHeight = (LocalConfiguration.current.screenHeightDp * 0.4f).dp.coerceAtMost(360.dp)
    AlertDialog(
        onDismissRequest = onDismiss,
        // SMCPKG_SUPPORT>>>Cursor112
        modifier = Modifier.padding(safeDrawingPadding()),
        // SMCPKG_SUPPORT<<<Cursor112
        title = { Text(stringResource(R.string.catalog_languages)) },
        text = {
            LazyColumn(Modifier.heightIn(max = listHeight)) {
                item(key = "all") {
                    TextButton(onClick = { onSelect("") }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.catalog_language_all))
                    }
                }
                items(languages, key = { it.name }) { language ->
                    val mark = if (language.name.equals(selected, true)) "  ✓" else ""
                    TextButton(onClick = { onSelect(language.name) }, modifier = Modifier.fillMaxWidth()) {
                        Text(language.name + mark, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun catalogLabels(): Map<String, String> = mapOf(
    CatalogTopics.POP to stringResource(R.string.catalog_pop),
    CatalogTopics.NEWS_TALK to stringResource(R.string.catalog_news_talk),
    CatalogTopics.MUSIC to stringResource(R.string.catalog_music),
    CatalogTopics.DECADE to stringResource(R.string.catalog_decades),
    CatalogTopics.LANGUAGE to stringResource(R.string.catalog_languages),
    CatalogTopics.NEWS to stringResource(R.string.catalog_news),
    CatalogTopics.TALK to stringResource(R.string.catalog_talk),
    CatalogTopics.SPORTS to stringResource(R.string.catalog_sports),
    CatalogTopics.RELIGIOUS to stringResource(R.string.catalog_religious),
    CatalogTopics.CLASSICAL to stringResource(R.string.catalog_classical),
    CatalogTopics.JAZZ to stringResource(R.string.catalog_jazz),
    CatalogTopics.ROCK to stringResource(R.string.catalog_rock),
    CatalogTopics.ELECTRONIC to stringResource(R.string.catalog_electronic),
    CatalogTopics.HIPHOP to stringResource(R.string.catalog_hiphop),
    CatalogTopics.COUNTRY to stringResource(R.string.catalog_country_music),
    CatalogTopics.LOFI to stringResource(R.string.catalog_lofi),
    CatalogTopics.WORLD to stringResource(R.string.catalog_world_music),
    CatalogTopics.D60 to stringResource(R.string.catalog_60s),
    CatalogTopics.D70 to stringResource(R.string.catalog_70s),
    CatalogTopics.D80 to stringResource(R.string.catalog_80s),
    CatalogTopics.D90 to stringResource(R.string.catalog_90s),
    CatalogTopics.D2000 to stringResource(R.string.catalog_2000s),
    CatalogTopics.OLDIES to stringResource(R.string.catalog_oldies),
)

@Composable
private fun countLabel(result: CountResult?): String {
    if (result == null) return ""
    return if (result.capped) {
        stringResource(R.string.catalog_count_plus, result.count)
    } else {
        stringResource(R.string.station_count, result.count)
    }
}

private fun cardCount(
    id: String,
    counts: Map<String, CountResult>,
    languages: List<CatalogLanguage>?,
): CountResult? {
    return when (id) {
        CatalogTopics.LANGUAGE -> {
            val known = languages ?: return null
            CountResult(
                count = known.sumOf { it.stationCount },
                capped = known.size >= 40,
            )
        }
        CatalogTopics.NEWS_TALK, CatalogTopics.MUSIC, CatalogTopics.DECADE ->
            CatalogTopics.parentCount(id, counts)
        else -> counts[id]
    }
}

private fun matchesQuery(station: RadioStation, query: String): Boolean {
    if (query.isEmpty()) return true
    return station.name.contains(query, ignoreCase = true) ||
        station.tags.contains(query, ignoreCase = true) ||
        station.country.contains(query, ignoreCase = true)
}

private sealed interface CatalogRow {
    data class StationRow(val station: RadioStation, val index: Int) : CatalogRow
    data object OtherHeader : CatalogRow
}

private fun CategoryHub.hasContent(): Boolean {
    return !votes.isNullOrEmpty() || !clicks.isNullOrEmpty() || !trend.isNullOrEmpty() ||
        counts.isNotEmpty() || !languages.isNullOrEmpty()
}
