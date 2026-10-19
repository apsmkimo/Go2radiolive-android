package com.apsmkimo.go2radiolive.presentation.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.presentation.ui.components.AdMobBanner
import com.apsmkimo.go2radiolive.presentation.ui.components.BrowseRow
import com.apsmkimo.go2radiolive.presentation.ui.components.BrowseRowList
import com.apsmkimo.go2radiolive.presentation.ui.components.CategoryList
import com.apsmkimo.go2radiolive.presentation.ui.components.MiniPlayer
import com.apsmkimo.go2radiolive.presentation.ui.components.SearchBarRow
import com.apsmkimo.go2radiolive.presentation.ui.components.StationList
import androidx.compose.ui.platform.LocalContext

private data class TabSpec(
    val tab: BrowseTab,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as Go2RadioApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // SMCPKG_SUPPORT>>>Cursor021
    val audioSettings by AudioSettingsRepository.settings.collectAsStateWithLifecycle()
    var audioPanelExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(audioSettings.showPanelButton) {
        if (!audioSettings.showPanelButton) {
            audioPanelExpanded = false
        }
    }
    // SMCPKG_SUPPORT<<<Cursor021

    val tabs = listOf(
        TabSpec(BrowseTab.LOCAL, R.string.tab_local, Icons.Outlined.Home),
        TabSpec(BrowseTab.WORLD, R.string.tab_world, Icons.Outlined.Public),
        // SMCPKG_SUPPORT>>>Cursor069
        // TabSpec(BrowseTab.RESERVED, R.string.tab_reserved, Icons.Outlined.StarOutline),
        TabSpec(BrowseTab.PODCAST, R.string.tab_podcast, Icons.Outlined.Podcasts),
        // SMCPKG_SUPPORT<<<Cursor069
        TabSpec(BrowseTab.CATEGORIES, R.string.tab_categories, Icons.Outlined.Category),
        TabSpec(BrowseTab.FAVORITES, R.string.tab_favorites, Icons.Outlined.Favorite),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = when {
                        state.selectedTab == BrowseTab.CATEGORIES && state.selectedCategory != null ->
                            state.selectedCategory?.name
                        state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null ->
                            state.selectedCountry?.name
                        state.selectedTab == BrowseTab.PODCAST && state.selectedPodcast != null ->
                            state.selectedPodcast?.name
                        else -> null
                    } ?: stringResource(R.string.app_name)
                    Text(title)
                },
                navigationIcon = {
                    val onBack: (() -> Unit)? = when {
                        state.selectedTab == BrowseTab.CATEGORIES && state.selectedCategory != null ->
                            viewModel::onClearCategory
                        state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null ->
                            viewModel::onClearCountry
                        state.selectedTab == BrowseTab.PODCAST && state.selectedPodcast != null ->
                            viewModel::onClearPodcast
                        else -> null
                    }
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
                // SMCPKG_SUPPORT>>>Cursor022
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.settings),
                        )
                    }
                },
                // SMCPKG_SUPPORT<<<Cursor022
            )
        },
        bottomBar = {
            Column {
                // SMCPKG_SUPPORT>>>Cursor023
                // MiniPlayer(
                //     playback = state.playback,
                //     onTogglePlayPause = viewModel::onTogglePlayPause,
                // )
                MiniPlayer(
                    playback = state.playback,
                    audioSettings = audioSettings,
                    audioPanelExpanded = audioPanelExpanded,
                    onTogglePlayPause = viewModel::onTogglePlayPause,
                    onAudioPanelExpandedChange = { audioPanelExpanded = it },
                    onBassChange = AudioSettingsRepository::setBassDb,
                    onLowChange = AudioSettingsRepository::setLowDb,
                    onMidChange = AudioSettingsRepository::setMidDb,
                    onHighChange = AudioSettingsRepository::setHighDb,
                    onBalanceChange = AudioSettingsRepository::setBalance,
                    onResetTone = AudioSettingsRepository::resetTone,
                )
                // SMCPKG_SUPPORT<<<Cursor023
                NavigationBar {
                    tabs.forEach { spec ->
                        NavigationBarItem(
                            selected = state.selectedTab == spec.tab,
                            onClick = { viewModel.onTabSelected(spec.tab) },
                            icon = { Icon(spec.icon, contentDescription = stringResource(spec.labelRes)) },
                            label = {
                                Text(
                                    text = stringResource(spec.labelRes),
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                )
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AdMobBanner()
            SearchBarRow(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onRefresh = viewModel::refresh,
            )
            val errorText = when {
                state.errorResId != null -> {
                    val base = stringResource(state.errorResId!!)
                    if (state.errorDetail.isNullOrBlank()) base else base + "\n" + state.errorDetail
                }
                else -> state.playback.errorMessage
            }
            when {
                state.showingCategories -> {
                    CategoryList(
                        categories = state.visibleCategories,
                        isLoading = state.isLoading,
                        errorMessage = errorText,
                        onCategoryClick = viewModel::onCategorySelected,
                        onRetry = viewModel::refresh,
                    )
                }
                state.showingCountries -> {
                    BrowseRowList(
                        rows = state.visibleCountries.map { country ->
                            BrowseRow(
                                id = country.code,
                                title = country.name,
                                subtitle = context.getString(R.string.station_count, country.stationCount),
                            )
                        },
                        isLoading = state.isLoading,
                        errorMessage = errorText,
                        emptyText = stringResource(R.string.empty_countries),
                        onRowClick = { row ->
                            state.countries.firstOrNull { it.code == row.id }?.let(viewModel::onCountrySelected)
                        },
                        onRetry = viewModel::refresh,
                    )
                }
                state.showingPodcasts -> {
                    BrowseRowList(
                        rows = state.visiblePodcasts.map { show ->
                            BrowseRow(
                                id = show.id,
                                title = show.name,
                                subtitle = show.author,
                            )
                        },
                        isLoading = state.isLoading,
                        errorMessage = errorText,
                        emptyText = stringResource(R.string.empty_podcasts),
                        onRowClick = { row ->
                            state.podcasts.firstOrNull { it.id == row.id }?.let(viewModel::onPodcastSelected)
                        },
                        onRetry = viewModel::refresh,
                        header = if (state.podcastUsFallback) {
                            stringResource(R.string.podcast_us_fallback)
                        } else {
                            null
                        },
                        showNativeAds = true,
                    )
                }
                else -> {
                    val emptyText = when {
                        state.selectedTab == BrowseTab.FAVORITES -> stringResource(R.string.empty_favorites)
                        state.selectedTab == BrowseTab.PODCAST -> stringResource(R.string.empty_episodes)
                        else -> stringResource(R.string.empty_stations)
                    }
                    val nativeAds = state.selectedTab == BrowseTab.LOCAL ||
                        (state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null)
                    StationList(
                        stations = state.visibleStations,
                        favoriteIds = state.favoriteIds,
                        isLoading = state.isLoading,
                        errorMessage = errorText,
                        emptyText = emptyText,
                        onStationClick = viewModel::onStationClick,
                        onToggleFavorite = viewModel::onToggleFavorite,
                        onRetry = viewModel::refresh,
                        showNativeAds = nativeAds,
                    )
                }
            }
        }
    }
}
