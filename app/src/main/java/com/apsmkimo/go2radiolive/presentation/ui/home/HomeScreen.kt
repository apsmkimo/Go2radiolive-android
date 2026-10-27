package com.apsmkimo.go2radiolive.presentation.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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
import android.content.Intent
import android.os.SystemClock
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.ads.AdsEntitlement
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.CatalogLevel
import com.apsmkimo.go2radiolive.alarm.PlaybackSignals
import com.apsmkimo.go2radiolive.alarm.SleepTimerState
import com.apsmkimo.go2radiolive.alarm.WakeAlarmScheduler
import com.apsmkimo.go2radiolive.presentation.player.RadioPlaybackService
import com.apsmkimo.go2radiolive.presentation.ui.components.AdMobBanner
import com.apsmkimo.go2radiolive.presentation.ui.components.CatalogTab
import com.apsmkimo.go2radiolive.presentation.ui.components.AlarmSheet
import com.apsmkimo.go2radiolive.presentation.ui.components.sleepBadgeMinutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.apsmkimo.go2radiolive.presentation.ui.components.BrowseRow
import com.apsmkimo.go2radiolive.presentation.ui.components.BrowseRowList
import com.apsmkimo.go2radiolive.presentation.ui.components.MiniPlayer
import com.apsmkimo.go2radiolive.presentation.ui.components.PodcastRegionDialog
import com.apsmkimo.go2radiolive.presentation.ui.components.SearchBarRow
import com.apsmkimo.go2radiolive.presentation.ui.components.StationList
import com.apsmkimo.go2radiolive.presentation.ui.bottomSafePadding
import com.apsmkimo.go2radiolive.presentation.ui.screenContentInsets
import com.apsmkimo.go2radiolive.presentation.ui.topSafePadding
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
    // SMCPKG_SUPPORT>>>Cursor113
    val adsRemoved by AdsEntitlement.removed.collectAsStateWithLifecycle()
    // SMCPKG_SUPPORT<<<Cursor113
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val alarmStore = app.container.wakeAlarmRepository
    val alarm by alarmStore.alarm.collectAsStateWithLifecycle()
    val lastStation by alarmStore.lastStation.collectAsStateWithLifecycle()
    val sleep by PlaybackSignals.sleep.collectAsStateWithLifecycle()
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val canExactAlarm = lifecycleState.isAtLeast(Lifecycle.State.STARTED) &&
        WakeAlarmScheduler.canScheduleExact(context)
    var alarmOpen by rememberSaveable { mutableStateOf(false) }
    var badgeNow by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sleep.active, sleep.endsAtElapsedMs, sleep.endOfEpisode) {
        if (!sleep.active || sleep.endOfEpisode) return@LaunchedEffect
        while (true) {
            badgeNow = SystemClock.elapsedRealtime()
            delay(1_000)
        }
    }
    val updatedText = stringResource(R.string.refresh_updated)
    val failedText = stringResource(R.string.load_failed)
    val cachedPodcastText = stringResource(R.string.podcast_cached_error)
    val cachedCatalogText = stringResource(R.string.catalog_cached_error)
    val retryText = stringResource(R.string.retry_action)
    LaunchedEffect(state.userRefreshEvent) {
        if (state.userRefreshEvent == 0) return@LaunchedEffect
        val message = if (state.userRefreshOk) updatedText else failedText
        snackbarHostState.showSnackbar(message)
    }
    // SMCPKG_SUPPORT>>>Cursor103
    LaunchedEffect(state.podcastStaleEvent) {
        if (state.podcastStaleEvent == 0) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = cachedPodcastText,
            actionLabel = retryText,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onRefreshClick()
        }
    }
    LaunchedEffect(state.catalogStaleEvent) {
        if (state.catalogStaleEvent == 0) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = cachedCatalogText,
            actionLabel = retryText,
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.onRefreshClick()
        }
    }
    // SMCPKG_SUPPORT<<<Cursor103
    // SMCPKG_SUPPORT>>>Cursor021
    val audioSettings by AudioSettingsRepository.settings.collectAsStateWithLifecycle()
    var audioPanelExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(audioSettings.showPanelButton) {
        if (!audioSettings.showPanelButton) {
            audioPanelExpanded = false
        }
    }
    // SMCPKG_SUPPORT<<<Cursor021

    // SMCPKG_SUPPORT>>>Cursor112
    val nestedBack: (() -> Unit)? = when {
        state.selectedTab == BrowseTab.CATEGORIES && state.catalogLevel != CatalogLevel.HUB ->
            viewModel::onCatalogBack
        state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null ->
            viewModel::onClearCountry
        state.selectedTab == BrowseTab.PODCAST && state.selectedPodcast != null ->
            viewModel::onClearPodcast
        else -> null
    }
    BackHandler(enabled = nestedBack != null) {
        nestedBack?.invoke()
    }
    // SMCPKG_SUPPORT<<<Cursor112

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
        // SMCPKG_SUPPORT>>>Cursor112
        contentWindowInsets = screenContentInsets(),
        // SMCPKG_SUPPORT<<<Cursor112
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                // SMCPKG_SUPPORT>>>Cursor112
                modifier = Modifier.padding(topSafePadding()),
                windowInsets = WindowInsets(0, 0, 0, 0),
                // SMCPKG_SUPPORT<<<Cursor112
                title = {
                    val title = when {
                        state.selectedTab == BrowseTab.CATEGORIES && state.catalogTitle.isNotBlank() ->
                            state.catalogTitle
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
                        state.selectedTab == BrowseTab.CATEGORIES && state.catalogLevel != CatalogLevel.HUB ->
                            viewModel::onCatalogBack
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
            // SMCPKG_SUPPORT>>>Cursor112
            Column(modifier = Modifier.padding(bottomSafePadding())) {
            // SMCPKG_SUPPORT<<<Cursor112
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
                // SMCPKG_SUPPORT>>>Cursor112
                NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                // SMCPKG_SUPPORT<<<Cursor112
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
            // SMCPKG_SUPPORT>>>Cursor113
            // AdMobBanner()
            if (!adsRemoved) {
                AdMobBanner()
            }
            // SMCPKG_SUPPORT<<<Cursor113
            // SMCPKG_SUPPORT>>>Cursor115
            // if (state.isLoading) {
            //     LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            // }
            val podcastQueryActive = state.showingPodcasts && state.searchQuery.trim().isNotEmpty()
            val screenBusy = if (podcastQueryActive) state.podcastSearchLoading else state.isLoading
            if (screenBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            // SMCPKG_SUPPORT<<<Cursor115
            // SMCPKG_SUPPORT>>>Cursor092
            var regionPickerOpen by rememberSaveable { mutableStateOf(false) }
            SearchBarRow(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onRefresh = viewModel::onRefreshClick,
                // SMCPKG_SUPPORT>>>Cursor115
                // isRefreshing = state.isLoading,
                isRefreshing = screenBusy,
                hintRes = if (state.showingPodcasts) R.string.podcast_search_hint else R.string.search_hint,
                // SMCPKG_SUPPORT<<<Cursor115
                onPickPodcastRegion = if (state.selectedTab == BrowseTab.PODCAST) {
                    { regionPickerOpen = true }
                } else {
                    null
                },
                onAlarm = { alarmOpen = true },
                sleepBadge = sleepBadgeMinutes(sleep, badgeNow),
            )
            if (regionPickerOpen) {
                PodcastRegionDialog(
                    selectedCode = state.podcastRegionCode,
                    deviceRegionCode = state.deviceRegionCode,
                    onDismiss = { regionPickerOpen = false },
                    onSelect = { code ->
                        regionPickerOpen = false
                        viewModel.onPodcastRegionSelected(code)
                    },
                )
            }
            // SearchBarRow(
            //     query = state.searchQuery,
            //     onQueryChange = viewModel::onSearchQueryChange,
            //     onRefresh = viewModel::refresh,
            // )
            // SMCPKG_SUPPORT<<<Cursor092
            if (alarmOpen) {
                AlarmSheet(
                    alarm = alarm,
                    lastStation = lastStation,
                    favorites = state.favoriteStations,
                    playback = state.playback,
                    sleep = sleep,
                    canExactAlarm = canExactAlarm,
                    onDismiss = { alarmOpen = false },
                    onSaveAlarm = { next ->
                        scope.launch {
                            alarmStore.save(next)
                            WakeAlarmScheduler.apply(context, next)
                            alarmOpen = false
                            snackbarHostState.showSnackbar(context.getString(R.string.alarm_saved))
                        }
                    },
                    onStartSleepMinutes = { minutes ->
                        PlaybackSignals.updateSleep(
                            SleepTimerState(
                                active = true,
                                endsAtElapsedMs = SystemClock.elapsedRealtime() + minutes * 60_000L,
                            ),
                        )
                        context.startService(
                            Intent(context, RadioPlaybackService::class.java).apply {
                                action = RadioPlaybackService.ACTION_SLEEP_MINUTES
                                putExtra(RadioPlaybackService.EXTRA_MINUTES, minutes)
                            },
                        )
                        alarmOpen = false
                    },
                    onStartSleepEpisode = {
                        PlaybackSignals.updateSleep(SleepTimerState(active = true, endOfEpisode = true))
                        context.startService(
                            Intent(context, RadioPlaybackService::class.java).apply {
                                action = RadioPlaybackService.ACTION_SLEEP_EPISODE
                            },
                        )
                        alarmOpen = false
                    },
                    onCancelSleep = {
                        PlaybackSignals.updateSleep(SleepTimerState())
                        context.startService(
                            Intent(context, RadioPlaybackService::class.java).apply {
                                action = RadioPlaybackService.ACTION_SLEEP_CANCEL
                            },
                        )
                    },
                    onRequestExactPermission = {
                        WakeAlarmScheduler.exactAlarmSettingsIntent(context)?.let(context::startActivity)
                    },
                )
            }
            val errorText = when {
                state.errorResId != null -> stringResource(state.errorResId!!)
                else -> state.playback.errorMessage
            }
            val hasItems = when {
                state.selectedTab == BrowseTab.CATEGORIES && state.catalogLevel == CatalogLevel.LIST ->
                    state.stations.isNotEmpty() || state.catalogOthers.isNotEmpty()
                state.selectedTab == BrowseTab.CATEGORIES -> state.catalogHub != null
                state.showingCountries -> state.visibleCountries.isNotEmpty()
                state.showingPodcasts -> state.visiblePodcasts.isNotEmpty()
                else -> state.visibleStations.isNotEmpty()
            }
            // SMCPKG_SUPPORT>>>Cursor115
            // if (!state.isLoading && state.errorResId != null && hasItems && errorText != null) {
            if (!podcastQueryActive && !state.isLoading && state.errorResId != null && hasItems && errorText != null) {
            // SMCPKG_SUPPORT<<<Cursor115
                TextButton(onClick = viewModel::onRefreshClick, modifier = Modifier.fillMaxWidth()) {
                    Text(errorText)
                }
            }
            when {
                state.selectedTab == BrowseTab.CATEGORIES -> {
                    CatalogTab(
                        scope = state.catalogScope,
                        level = state.catalogLevel,
                        groupId = state.catalogGroup,
                        listKind = state.catalogListKind,
                        hub = state.catalogHub,
                        stations = state.visibleStations,
                        others = state.catalogOthers.filter { station ->
                            val q = state.searchQuery.trim()
                            q.isEmpty() || station.name.contains(q, true) ||
                                station.tags.contains(q, true) || station.country.contains(q, true)
                        },
                        countries = state.countries,
                        sort = state.catalogSort,
                        languageFilter = state.catalogLanguageFilter,
                        highBitrate = state.catalogHighBitrate,
                        query = state.searchQuery,
                        isLoading = state.isLoading,
                        errorMessage = if (state.errorResId != null) errorText else null,
                        favoriteIds = state.favoriteIds,
                        scrollToken = state.scrollToken,
                        onScope = viewModel::onCatalogScopeSelected,
                        onOpenGroup = viewModel::onCatalogGroup,
                        onOpenTopic = viewModel::onCatalogTopic,
                        onOpenLanguage = viewModel::onCatalogLanguage,
                        onOpenRank = viewModel::onCatalogRank,
                        onSort = viewModel::onCatalogSort,
                        onLanguageFilter = viewModel::onCatalogLanguageFilter,
                        onHighBitrate = viewModel::onCatalogHighBitrate,
                        onStation = viewModel::onStationClick,
                        onFavorite = viewModel::onToggleFavorite,
                        onRetry = viewModel::onRefreshClick,
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
                        onRetry = viewModel::onRefreshClick,
                        scrollToken = state.scrollToken,
                    )
                }
                state.showingPodcasts -> {
                    // SMCPKG_SUPPORT>>>Cursor115
                    // BrowseRowList(
                    //     rows = state.visiblePodcasts.map { show ->
                    //         BrowseRow(
                    //             id = show.id,
                    //             title = show.name,
                    //             subtitle = show.author,
                    //         )
                    //     },
                    //     isLoading = state.isLoading,
                    //     errorMessage = errorText,
                    //     emptyText = stringResource(R.string.empty_podcasts),
                    //     onRowClick = { row ->
                    //         state.podcasts.firstOrNull { it.id == row.id }?.let(viewModel::onPodcastSelected)
                    //     },
                    //     onRetry = viewModel::onRefreshClick,
                    //     scrollToken = state.scrollToken,
                    //     header = if (state.podcastUsFallback) {
                    //         stringResource(R.string.podcast_us_fallback)
                    //     } else {
                    //         null
                    //     },
                    val podcastError = if (podcastQueryActive) {
                        state.podcastSearchErrorResId?.let { stringResource(it) }
                    } else {
                        errorText
                    }
                    BrowseRowList(
                        rows = state.visiblePodcasts.map { show ->
                            BrowseRow(
                                id = show.id,
                                title = show.name,
                                subtitle = show.author,
                            )
                        },
                        isLoading = if (podcastQueryActive) state.podcastSearchLoading else state.isLoading,
                        errorMessage = podcastError,
                        emptyText = stringResource(
                            if (podcastQueryActive) R.string.podcast_search_empty else R.string.empty_podcasts,
                        ),
                        onRowClick = { row ->
                            state.visiblePodcasts.firstOrNull { it.id == row.id }?.let(viewModel::onPodcastSelected)
                        },
                        onRetry = viewModel::onRefreshClick,
                        scrollToken = state.scrollToken,
                        header = if (!podcastQueryActive && state.podcastUsFallback) {
                            stringResource(R.string.podcast_us_fallback)
                        } else {
                            null
                        },
                    // SMCPKG_SUPPORT<<<Cursor115
                        // SMCPKG_SUPPORT>>>Cursor113
                        // showNativeAds = true,
                        showNativeAds = !adsRemoved,
                        // SMCPKG_SUPPORT<<<Cursor113
                    )
                }
                else -> {
                    val emptyText = when {
                        state.selectedTab == BrowseTab.FAVORITES -> stringResource(R.string.empty_favorites)
                        state.selectedTab == BrowseTab.PODCAST -> stringResource(R.string.empty_episodes)
                        else -> stringResource(R.string.empty_stations)
                    }
                    // SMCPKG_SUPPORT>>>Cursor113
                    // val nativeAds = state.selectedTab == BrowseTab.LOCAL ||
                    //     (state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null)
                    val nativeAds = !adsRemoved && (
                        state.selectedTab == BrowseTab.LOCAL ||
                            (state.selectedTab == BrowseTab.WORLD && state.selectedCountry != null)
                        )
                    // SMCPKG_SUPPORT<<<Cursor113
                    StationList(
                        stations = state.visibleStations,
                        favoriteIds = state.favoriteIds,
                        isLoading = state.isLoading,
                        errorMessage = errorText,
                        emptyText = emptyText,
                        onStationClick = viewModel::onStationClick,
                        onToggleFavorite = viewModel::onToggleFavorite,
                        onRetry = viewModel::onRefreshClick,
                        scrollToken = state.scrollToken,
                        showNativeAds = nativeAds,
                    )
                }
            }
        }
    }
}
