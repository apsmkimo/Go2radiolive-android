package com.apsmkimo.go2radiolive.presentation.ui.home

import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.data.local.CategoryCatalogStore
import com.apsmkimo.go2radiolive.data.local.PodcastCatalogStore
import com.apsmkimo.go2radiolive.data.local.PodcastRegionRepository
import com.apsmkimo.go2radiolive.data.remote.CatalogTopics
import com.apsmkimo.go2radiolive.data.remote.isPodcastCacheStale
import com.apsmkimo.go2radiolive.data.remote.podcastStorefront
import com.apsmkimo.go2radiolive.data.repository.CategoryCatalogRepository
import com.apsmkimo.go2radiolive.di.AppContainer
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.CatalogLevel
import com.apsmkimo.go2radiolive.domain.model.CatalogPage
import com.apsmkimo.go2radiolive.domain.model.CatalogSort
import com.apsmkimo.go2radiolive.domain.model.CategoryHub
import com.apsmkimo.go2radiolive.domain.model.CountryOption
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.PodcastChart
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.usecase.GetCategoriesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetCountriesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetPodcastEpisodesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetPodcastShowsUseCase
import com.apsmkimo.go2radiolive.domain.usecase.GetStationsUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ObserveFavoritesUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ResolveStreamUrlUseCase
import com.apsmkimo.go2radiolive.domain.usecase.ToggleFavoriteUseCase
import com.apsmkimo.go2radiolive.presentation.player.RadioPlayerController
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val selectedTab: BrowseTab = BrowseTab.LOCAL,
    val searchQuery: String = "",
    val stations: List<RadioStation> = emptyList(),
    val categories: List<StationCategory> = emptyList(),
    val selectedCategory: StationCategory? = null,
    val countries: List<CountryOption> = emptyList(),
    val selectedCountry: CountryOption? = null,
    val podcasts: List<PodcastShow> = emptyList(),
    val podcastUsFallback: Boolean = false,
    /** Empty means automatic (device region). Otherwise a lowercase Apple storefront. */
    val podcastRegionCode: String = "",
    val deviceRegionCode: String = "",
    val selectedPodcast: PodcastShow? = null,
    val favoriteIds: Set<String> = emptySet(),
    val favoriteStations: List<RadioStation> = emptyList(),
    val isLoading: Boolean = false,
    val userRefreshEvent: Int = 0,
    val userRefreshOk: Boolean = true,
    /** Bumped when a podcast refresh fails but a saved list is still on screen. */
    val podcastStaleEvent: Int = 0,
    // SMCPKG_SUPPORT>>>Cursor106
    val catalogScope: String = "",
    val catalogLevel: CatalogLevel = CatalogLevel.HUB,
    val catalogGroup: String? = null,
    val catalogGroupTitle: String = "",
    val catalogListKind: String? = null,
    val catalogTitle: String = "",
    val catalogOpenedFromGroup: Boolean = false,
    val catalogHub: CategoryHub? = null,
    val catalogOthers: List<RadioStation> = emptyList(),
    val catalogSort: CatalogSort = CatalogSort.CLICKS,
    val catalogLanguageFilter: String = "",
    val catalogHighBitrate: Boolean = false,
    val catalogStaleEvent: Int = 0,
    // SMCPKG_SUPPORT<<<Cursor106
    val scrollToken: Int = 0,
    @StringRes val errorResId: Int? = null,
    val playback: PlaybackState = PlaybackState(),
) {
    val visibleStations: List<RadioStation>
        get() {
            val source = if (selectedTab == BrowseTab.FAVORITES) favoriteStations else stations
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                source
            } else {
                source.filter { station ->
                    station.name.contains(query, ignoreCase = true) ||
                        station.tags.contains(query, ignoreCase = true) ||
                        station.country.contains(query, ignoreCase = true)
                }
            }
        }

    val visibleCategories: List<StationCategory>
        get() = filterByName(categories, searchQuery) { it.name }

    val visibleCountries: List<CountryOption>
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                countries
            } else {
                countries.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.code.contains(query, ignoreCase = true)
                }
            }
        }

    val visiblePodcasts: List<PodcastShow>
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                podcasts
            } else {
                podcasts.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.author.contains(query, ignoreCase = true)
                }
            }
        }

    val showingCategories: Boolean
        get() = selectedTab == BrowseTab.CATEGORIES && selectedCategory == null

    val showingCountries: Boolean
        get() = selectedTab == BrowseTab.WORLD && selectedCountry == null

    val showingPodcasts: Boolean
        get() = selectedTab == BrowseTab.PODCAST && selectedPodcast == null
}

private fun <T> filterByName(items: List<T>, query: String, name: (T) -> String): List<T> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) items else items.filter { name(it).contains(trimmed, ignoreCase = true) }
}

class HomeViewModel(
    private val getStations: GetStationsUseCase,
    private val getCategories: GetCategoriesUseCase,
    private val getCountries: GetCountriesUseCase,
    private val getPodcastShows: GetPodcastShowsUseCase,
    private val getPodcastEpisodes: GetPodcastEpisodesUseCase,
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val resolveStreamUrl: ResolveStreamUrlUseCase,
    private val playerController: RadioPlayerController,
    private val deviceRegion: () -> String,
    private val podcastRegion: PodcastRegionRepository,
    private val podcastCache: PodcastCatalogStore,
    private val categoryCatalog: CategoryCatalogRepository,
    private val categoryCache: CategoryCatalogStore,
) : ViewModel() {

    private val internalState = MutableStateFlow(HomeUiState())
    private var loadJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        internalState,
        observeFavorites(),
        playerController.playbackState,
    ) { ui, favorites, playback ->
        ui.copy(
            favoriteStations = favorites,
            favoriteIds = favorites.map { it.stationUuid }.toSet(),
            playback = playback,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(),
    )

    init {
        viewModelScope.launch {
            podcastRegion.awaitLoaded()
            internalState.update {
                it.copy(
                    podcastRegionCode = podcastRegion.selection.value,
                    deviceRegionCode = deviceRegion(),
                )
            }
        }
        refresh()
    }

    fun onTabSelected(tab: BrowseTab) {
        internalState.update {
            it.copy(
                selectedTab = tab,
                selectedCategory = if (tab == BrowseTab.CATEGORIES) it.selectedCategory else null,
                selectedCountry = if (tab == BrowseTab.WORLD) it.selectedCountry else null,
                selectedPodcast = if (tab == BrowseTab.PODCAST) it.selectedPodcast else null,
                stations = emptyList(),
                searchQuery = "",
                errorResId = null,
            )
        }
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        internalState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: StationCategory) {
        internalState.update {
            it.copy(selectedCategory = category, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun onClearCategory() {
        internalState.update {
            it.copy(selectedCategory = null, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    // SMCPKG_SUPPORT>>>Cursor106
    fun onCatalogScopeSelected(code: String) {
        internalState.update {
            it.copy(
                catalogScope = code,
                catalogLevel = CatalogLevel.HUB,
                catalogGroup = null,
                catalogGroupTitle = "",
                catalogListKind = null,
                catalogTitle = "",
                catalogOpenedFromGroup = false,
                catalogHub = null,
                stations = emptyList(),
                catalogOthers = emptyList(),
                searchQuery = "",
                errorResId = null,
            )
        }
        refresh()
    }

    fun onCatalogGroup(id: String, title: String) {
        internalState.update {
            it.copy(
                catalogLevel = CatalogLevel.GROUP,
                catalogGroup = id,
                catalogGroupTitle = title,
                catalogTitle = title,
                catalogListKind = null,
                searchQuery = "",
                errorResId = null,
            )
        }
    }

    fun onCatalogTopic(id: String, title: String) {
        openCatalogList("topic:$id", title, fromGroup = internalState.value.catalogLevel == CatalogLevel.GROUP)
    }

    fun onCatalogLanguage(name: String) {
        openCatalogList(
            "lang:$name",
            name,
            fromGroup = internalState.value.catalogLevel == CatalogLevel.GROUP,
        )
    }

    fun onCatalogRank(kind: String, title: String) {
        openCatalogList(kind, title, fromGroup = false)
    }

    fun onCatalogBack() {
        internalState.update {
            when (it.catalogLevel) {
                CatalogLevel.LIST -> if (it.catalogOpenedFromGroup) {
                    it.copy(
                        catalogLevel = CatalogLevel.GROUP,
                        catalogTitle = it.catalogGroupTitle,
                        catalogListKind = null,
                        stations = emptyList(),
                        catalogOthers = emptyList(),
                        searchQuery = "",
                        errorResId = null,
                    )
                } else {
                    it.copy(
                        catalogLevel = CatalogLevel.HUB,
                        catalogTitle = "",
                        catalogListKind = null,
                        catalogOpenedFromGroup = false,
                        stations = emptyList(),
                        catalogOthers = emptyList(),
                        searchQuery = "",
                        errorResId = null,
                    )
                }
                CatalogLevel.GROUP -> it.copy(
                    catalogLevel = CatalogLevel.HUB,
                    catalogGroup = null,
                    catalogGroupTitle = "",
                    catalogTitle = "",
                    searchQuery = "",
                    errorResId = null,
                )
                CatalogLevel.HUB -> it
            }
        }
    }

    fun onCatalogSort(sort: CatalogSort) {
        if (sort == internalState.value.catalogSort) return
        internalState.update { it.copy(catalogSort = sort) }
        refresh()
    }

    fun onCatalogLanguageFilter(language: String) {
        if (language == internalState.value.catalogLanguageFilter) return
        internalState.update { it.copy(catalogLanguageFilter = language) }
        refresh()
    }

    fun onCatalogHighBitrate(enabled: Boolean) {
        if (enabled == internalState.value.catalogHighBitrate) return
        internalState.update { it.copy(catalogHighBitrate = enabled) }
        refresh()
    }

    private fun openCatalogList(kind: String, title: String, fromGroup: Boolean) {
        internalState.update {
            it.copy(
                catalogLevel = CatalogLevel.LIST,
                catalogListKind = kind,
                catalogTitle = title,
                catalogOpenedFromGroup = fromGroup,
                catalogSort = CatalogSort.CLICKS,
                catalogLanguageFilter = "",
                catalogHighBitrate = false,
                stations = emptyList(),
                catalogOthers = emptyList(),
                searchQuery = "",
                errorResId = null,
            )
        }
        refresh()
    }
    // SMCPKG_SUPPORT<<<Cursor106

    fun onCountrySelected(country: CountryOption) {
        internalState.update {
            it.copy(selectedCountry = country, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun onClearCountry() {
        internalState.update {
            it.copy(selectedCountry = null, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun onPodcastSelected(show: PodcastShow) {
        internalState.update {
            it.copy(selectedPodcast = show, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun onClearPodcast() {
        internalState.update {
            it.copy(selectedPodcast = null, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    // SMCPKG_SUPPORT>>>Cursor092
    fun onPodcastRegionSelected(code: String) {
        viewModelScope.launch {
            podcastRegion.setSelection(code)
            val saved = podcastRegion.selection.value
            val device = deviceRegion()
            val region = saved.ifBlank { device }
            val cached = withContext(Dispatchers.IO) {
                podcastCache.read(podcastStorefront(region))
            }
            internalState.update {
                it.copy(
                    podcastRegionCode = saved,
                    deviceRegionCode = device,
                    selectedPodcast = null,
                    podcasts = cached?.shows.orEmpty(),
                    stations = emptyList(),
                    searchQuery = "",
                    errorResId = null,
                    podcastUsFallback = cached?.usedUsFallback == true,
                )
            }
            refresh()
        }
    }
    // SMCPKG_SUPPORT<<<Cursor092

    fun onRefreshClick() {
        refresh(userInitiated = true)
    }

    fun refresh(userInitiated: Boolean = false) {
        val snapshot = internalState.value
        if (snapshot.selectedTab == BrowseTab.FAVORITES) {
            if (!userInitiated) {
                internalState.update { it.copy(isLoading = false, errorResId = null) }
                return
            }
            loadJob?.cancel()
            loadJob = viewModelScope.launch {
                internalState.update { it.copy(isLoading = true, errorResId = null) }
                delay(300)
                finishLoad(userInitiated = true, ok = true) { it }
            }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val podcastList = snapshot.selectedTab == BrowseTab.PODCAST && snapshot.selectedPodcast == null
            val catalog = snapshot.selectedTab == BrowseTab.CATEGORIES
            if (!podcastList && !catalog) {
                internalState.update { it.copy(isLoading = true, errorResId = null) }
            }
            runCatching {
                when {
                    snapshot.selectedTab == BrowseTab.CATEGORIES -> {
                        // SMCPKG_SUPPORT>>>Cursor106
                        // val categories = getCategories(bypassCache = userInitiated)
                        // finishLoad(userInitiated, ok = true) {
                        //     it.copy(categories = categories, stations = emptyList())
                        // }
                        loadCatalog(userInitiated)
                        // SMCPKG_SUPPORT<<<Cursor106
                    }
                    snapshot.selectedTab == BrowseTab.WORLD && snapshot.selectedCountry == null -> {
                        val countries = getCountries(bypassCache = userInitiated)
                        finishLoad(userInitiated, ok = true) {
                            it.copy(countries = countries, stations = emptyList())
                        }
                    }
                    snapshot.selectedTab == BrowseTab.PODCAST && snapshot.selectedPodcast == null -> {
                        // SMCPKG_SUPPORT>>>Cursor092
                        // podcastRegion.awaitLoaded()
                        // val saved = podcastRegion.selection.value
                        // val region = saved.ifBlank { deviceRegion() }
                        // val chart: PodcastChart = getPodcastShows(region, bypassCache = userInitiated)
                        // finishLoad(userInitiated, ok = true) {
                        //     it.copy(
                        //         podcasts = chart.shows,
                        //         podcastUsFallback = chart.usedUsFallback,
                        //         podcastRegionCode = saved,
                        //         deviceRegionCode = deviceRegion(),
                        //         stations = emptyList(),
                        //     )
                        // }
                        // SMCPKG_SUPPORT<<<Cursor092
                        // SMCPKG_SUPPORT>>>Cursor103
                        loadPodcastList(userInitiated)
                        // SMCPKG_SUPPORT<<<Cursor103
                    }
                    snapshot.selectedTab == BrowseTab.PODCAST && snapshot.selectedPodcast != null -> {
                        // SMCPKG_SUPPORT>>>Cursor092
                        podcastRegion.awaitLoaded()
                        val selectedRegion = podcastRegion.selection.value.ifBlank { deviceRegion() }
                        val region = if (snapshot.podcastUsFallback) "us" else selectedRegion
                        // val region = if (snapshot.podcastUsFallback) "us" else deviceRegion()
                        // SMCPKG_SUPPORT<<<Cursor092
                        val episodes = getPodcastEpisodes(snapshot.selectedPodcast, region, bypassCache = userInitiated)
                        finishLoad(userInitiated, ok = true) { it.copy(stations = episodes) }
                    }
                    else -> {
                        val country = if (snapshot.selectedTab == BrowseTab.LOCAL) {
                            deviceRegion()
                        } else {
                            snapshot.selectedCountry?.code
                        }
                        val stations = getStations(
                            snapshot.selectedTab,
                            snapshot.selectedCategory?.name,
                            country,
                            bypassCache = userInitiated,
                        )
                        finishLoad(userInitiated, ok = true) { it.copy(stations = stations) }
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                Log.w(TAG, "load failed", error)
                finishLoad(userInitiated, ok = false) {
                    it.copy(
                        errorResId = R.string.load_failed,
                        podcastUsFallback = false,
                    )
                }
            }
        }
    }

    // SMCPKG_SUPPORT>>>Cursor103
    private suspend fun loadPodcastList(userInitiated: Boolean) {
        podcastRegion.awaitLoaded()
        val saved = podcastRegion.selection.value
        val device = deviceRegion()
        val region = saved.ifBlank { device }
        val regionKey = podcastStorefront(region)
        val cached = withContext(Dispatchers.IO) { podcastCache.read(regionKey) }
        val cachedShows = cached?.shows.orEmpty()
        val refreshNetwork = userInitiated ||
            cached == null ||
            cachedShows.isEmpty() ||
            isPodcastCacheStale(cached.savedAtMs, System.currentTimeMillis())
        if (cachedShows.isNotEmpty()) {
            internalState.update {
                it.copy(
                    podcasts = cachedShows,
                    podcastUsFallback = cached?.usedUsFallback == true,
                    podcastRegionCode = saved,
                    deviceRegionCode = device,
                    stations = emptyList(),
                    errorResId = null,
                    isLoading = refreshNetwork,
                )
            }
        } else {
            internalState.update {
                it.copy(
                    isLoading = true,
                    errorResId = null,
                    podcastRegionCode = saved,
                    deviceRegionCode = device,
                    podcasts = emptyList(),
                    podcastUsFallback = false,
                    stations = emptyList(),
                )
            }
        }
        if (!refreshNetwork) {
            finishLoad(userInitiated = false, ok = true) { it }
            return
        }
        var last: PodcastChart? = null
        try {
            getPodcastShows(region, bypassCache = userInitiated).collect { chart ->
                if (chart.shows.isEmpty()) return@collect
                last = chart
                val current = internalState.value.podcasts
                val samePrefix = chart.shows.size >= current.size &&
                    current.indices.all { index -> chart.shows[index].id == current[index].id }
                if (current.isEmpty() || chart.complete || samePrefix) {
                    internalState.update {
                        it.copy(
                            podcasts = chart.shows,
                            podcastUsFallback = chart.usedUsFallback,
                            podcastRegionCode = saved,
                            deviceRegionCode = device,
                            stations = emptyList(),
                            errorResId = null,
                        )
                    }
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (internalState.value.podcasts.isNotEmpty()) {
                notePodcastStale()
                return
            }
            throw error
        }
        val finalChart = last
        if (finalChart == null || finalChart.shows.isEmpty()) {
            if (internalState.value.podcasts.isNotEmpty()) {
                notePodcastStale()
                return
            }
            finishLoad(userInitiated, ok = false) {
                it.copy(errorResId = R.string.load_failed)
            }
            return
        }
        finishLoad(userInitiated, ok = true) {
            it.copy(
                podcasts = finalChart.shows,
                podcastUsFallback = finalChart.usedUsFallback,
                podcastRegionCode = saved,
                deviceRegionCode = device,
                stations = emptyList(),
            )
        }
        if (finalChart.complete) {
            withContext(Dispatchers.IO) {
                podcastCache.save(regionKey, finalChart)
                if (finalChart.usedUsFallback) {
                    podcastCache.save("us", finalChart.copy(usedUsFallback = false))
                }
            }
        }
    }

    private fun notePodcastStale() {
        finishLoad(userInitiated = false, ok = true) {
            it.copy(
                errorResId = R.string.podcast_cached_error,
                podcastStaleEvent = it.podcastStaleEvent + 1,
            )
        }
    }
    // SMCPKG_SUPPORT<<<Cursor103

    // SMCPKG_SUPPORT>>>Cursor106
    private suspend fun loadCatalog(userInitiated: Boolean) {
        val snapshot = internalState.value
        val scope = resolveCatalogScope(snapshot.catalogScope)
        if (snapshot.catalogScope != scope) {
            internalState.update { it.copy(catalogScope = scope) }
        }
        if (snapshot.catalogLevel == CatalogLevel.LIST && !snapshot.catalogListKind.isNullOrBlank()) {
            loadCatalogList(userInitiated, scope, snapshot)
        } else {
            loadCatalogHub(userInitiated, scope)
        }
    }

    private fun resolveCatalogScope(explicit: String): String {
        if (explicit == CatalogTopics.GLOBAL) return CatalogTopics.GLOBAL
        if (explicit.length == 2) return explicit.uppercase()
        val device = deviceRegion().trim().uppercase()
        return if (device.length == 2) device else "US"
    }

    private suspend fun loadCatalogHub(userInitiated: Boolean, scope: String) {
        val cached = withContext(Dispatchers.IO) { categoryCache.readHub(scope) }
        val stale = cached == null || isPodcastCacheStale(cached.savedAtMs, System.currentTimeMillis())
        if (cached != null) {
            internalState.update {
                it.copy(
                    catalogScope = scope,
                    catalogHub = cached.toHub(),
                    isLoading = userInitiated || stale,
                    errorResId = null,
                )
            }
            if (!userInitiated && !stale) {
                ensureCountries(false)
                finishLoad(false, ok = true) { it }
                return
            }
        } else {
            internalState.update {
                it.copy(catalogScope = scope, isLoading = true, errorResId = null)
            }
        }
        val hadHub = cached != null
        var latest: CategoryHub? = null
        try {
            coroutineScope {
                val countriesJob = async { ensureCountries(userInitiated) }
                categoryCatalog.hub(scopeCode(scope), bypassCache = userInitiated).collect { hub ->
                    latest = hub
                    val reveal = !hadHub || hub.complete
                    if (reveal) {
                        internalState.update {
                            it.copy(catalogScope = scope, catalogHub = hub, errorResId = null)
                        }
                    }
                }
                countriesJob.await()
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (internalState.value.catalogHub?.hasContent() == true) {
                noteCatalogStale()
                return
            }
            throw error
        }
        val finalHub = latest
        if (finalHub == null || !finalHub.hasContent()) {
            if (internalState.value.catalogHub?.hasContent() == true) {
                noteCatalogStale()
                return
            }
            finishLoad(userInitiated, ok = false) { it.copy(errorResId = R.string.load_failed) }
            return
        }
        finishLoad(userInitiated, ok = true) { it.copy(catalogScope = scope, catalogHub = finalHub) }
        if (finalHub.complete && finalHub.canCache()) {
            withContext(Dispatchers.IO) { categoryCache.saveHub(scope, finalHub) }
        }
    }

    private suspend fun loadCatalogList(userInitiated: Boolean, scope: String, snapshot: HomeUiState) {
        val kind = snapshot.catalogListKind.orEmpty()
        val sort = snapshot.catalogSort
        val language = snapshot.catalogLanguageFilter
        val high = snapshot.catalogHighBitrate
        val key = CatalogTopics.listKey(scope, kind, sort, language, high)
        val cached = withContext(Dispatchers.IO) { categoryCache.readList(key) }
        val stale = cached == null || isPodcastCacheStale(cached.savedAtMs, System.currentTimeMillis())
        if (cached != null) {
            internalState.update {
                it.copy(
                    stations = cached.stations.orEmpty(),
                    catalogOthers = cached.others.orEmpty(),
                    isLoading = userInitiated || stale,
                    errorResId = null,
                )
            }
            if (!userInitiated && !stale) {
                finishLoad(false, ok = true) { it }
                return
            }
        } else if (internalState.value.stations.isEmpty() && internalState.value.catalogOthers.isEmpty()) {
            internalState.update { it.copy(isLoading = true, errorResId = null) }
        } else {
            internalState.update { it.copy(isLoading = true, errorResId = null) }
        }
        val hadList = cached != null || internalState.value.stations.isNotEmpty()
        var latest: CatalogPage? = null
        try {
            categoryCatalog.page(
                countryCode = scopeCode(scope),
                kind = kind,
                sort = sort,
                language = language,
                highBitrate = high,
                bypassCache = userInitiated,
            ).collect { page ->
                latest = page
                val current = internalState.value.stations
                val reveal = current.isEmpty() || page.complete || page.stations.size >= current.size
                if (!hadList || reveal) {
                    if (current.isEmpty() || page.complete || page.stations.size >= current.size) {
                        internalState.update {
                            it.copy(
                                stations = page.stations,
                                catalogOthers = if (page.complete) page.others else it.catalogOthers,
                                errorResId = null,
                            )
                        }
                    }
                }
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            if (internalState.value.stations.isNotEmpty() || internalState.value.catalogOthers.isNotEmpty()) {
                noteCatalogStale()
                return
            }
            throw error
        }
        val finalPage = latest
        if (finalPage == null) {
            if (internalState.value.stations.isNotEmpty() || internalState.value.catalogOthers.isNotEmpty()) {
                noteCatalogStale()
                return
            }
            finishLoad(userInitiated, ok = false) { it.copy(errorResId = R.string.load_failed) }
            return
        }
        finishLoad(userInitiated, ok = true) {
            it.copy(stations = finalPage.stations, catalogOthers = finalPage.others)
        }
        if (finalPage.complete && (finalPage.stations.isNotEmpty() || finalPage.others.isNotEmpty())) {
            withContext(Dispatchers.IO) { categoryCache.saveList(key, finalPage) }
        }
    }

    private suspend fun ensureCountries(bypassCache: Boolean) {
        if (internalState.value.countries.isNotEmpty()) return
        val loaded = runCatching { getCountries(bypassCache) }.getOrDefault(emptyList())
        if (loaded.isNotEmpty()) {
            internalState.update { current ->
                if (current.countries.isNotEmpty()) current else current.copy(countries = loaded)
            }
        }
    }

    private fun noteCatalogStale() {
        finishLoad(userInitiated = false, ok = true) {
            it.copy(
                errorResId = R.string.catalog_cached_error,
                catalogStaleEvent = it.catalogStaleEvent + 1,
            )
        }
    }

    private fun scopeCode(scope: String): String? =
        if (scope == CatalogTopics.GLOBAL) null else scope

    private fun CategoryCatalogStore.HubEntry.toHub(): CategoryHub {
        return CategoryHub(
            votes = votes,
            clicks = clicks,
            trend = trend,
            counts = counts.orEmpty(),
            languages = languages,
            complete = true,
        )
    }

    private fun CategoryHub.hasContent(): Boolean {
        return !votes.isNullOrEmpty() || !clicks.isNullOrEmpty() || !trend.isNullOrEmpty() ||
            counts.isNotEmpty() || !languages.isNullOrEmpty()
    }

    /** A failed ranking, topic, or language list is omitted. Don't freeze that gap for 12 hours. */
    private fun CategoryHub.canCache(): Boolean {
        if (votes == null || clicks == null || trend == null || languages == null) return false
        return CatalogTopics.topicIds().all { it in counts }
    }
    // SMCPKG_SUPPORT<<<Cursor106

    private fun finishLoad(
        userInitiated: Boolean,
        ok: Boolean,
        block: (HomeUiState) -> HomeUiState,
    ) {
        internalState.update { current ->
            val next = block(current)
            next.copy(
                isLoading = false,
                userRefreshEvent = if (userInitiated) current.userRefreshEvent + 1 else current.userRefreshEvent,
                userRefreshOk = if (userInitiated) ok else current.userRefreshOk,
                scrollToken = if (userInitiated && ok) current.scrollToken + 1 else current.scrollToken,
            )
        }
    }

    fun onStationClick(station: RadioStation) {
        viewModelScope.launch {
            internalState.update { it.copy(errorResId = null) }
            val streamUrl = if (station.directStream) {
                station.streamUrl
            } else {
                runCatching { resolveStreamUrl(station) }.getOrDefault(station.streamUrl)
            }
            playerController.play(station, streamUrl)
        }
    }

    fun onToggleFavorite(station: RadioStation) {
        viewModelScope.launch {
            runCatching { toggleFavorite(station) }
        }
    }

    fun onTogglePlayPause() {
        if (uiState.value.playback.station == null) return
        playerController.togglePlayPause()
    }
}

private const val TAG = "HomeViewModel"

class HomeViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(
            getStations = container.getStationsUseCase,
            getCategories = container.getCategoriesUseCase,
            getCountries = container.getCountriesUseCase,
            getPodcastShows = container.getPodcastShowsUseCase,
            getPodcastEpisodes = container.getPodcastEpisodesUseCase,
            observeFavorites = container.observeFavoritesUseCase,
            toggleFavorite = container.toggleFavoriteUseCase,
            resolveStreamUrl = container.resolveStreamUrlUseCase,
            playerController = container.playerController,
            deviceRegion = container::deviceRegion,
            podcastRegion = container.podcastRegionRepository,
            podcastCache = container.podcastCatalogStore,
            categoryCatalog = container.categoryCatalogRepository,
            categoryCache = container.categoryCatalogStore,
        ) as T
    }
}
