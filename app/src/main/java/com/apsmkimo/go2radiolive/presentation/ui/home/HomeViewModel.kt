package com.apsmkimo.go2radiolive.presentation.ui.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.di.AppContainer
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
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
import kotlinx.coroutines.Job
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
    val selectedPodcast: PodcastShow? = null,
    val favoriteIds: Set<String> = emptySet(),
    val favoriteStations: List<RadioStation> = emptyList(),
    val isLoading: Boolean = false,
    @StringRes val errorResId: Int? = null,
    val errorDetail: String? = null,
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
                errorDetail = null,
            )
        }
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        internalState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: StationCategory) {
        internalState.update {
            it.copy(selectedCategory = category, stations = emptyList(), searchQuery = "", errorResId = null, errorDetail = null)
        }
        refresh()
    }

    fun onClearCategory() {
        internalState.update {
            it.copy(selectedCategory = null, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun onCountrySelected(country: CountryOption) {
        internalState.update {
            it.copy(selectedCountry = country, stations = emptyList(), searchQuery = "", errorResId = null, errorDetail = null)
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
            it.copy(selectedPodcast = show, stations = emptyList(), searchQuery = "", errorResId = null, errorDetail = null)
        }
        refresh()
    }

    fun onClearPodcast() {
        internalState.update {
            it.copy(selectedPodcast = null, stations = emptyList(), searchQuery = "", errorResId = null)
        }
        refresh()
    }

    fun refresh() {
        val snapshot = internalState.value
        if (snapshot.selectedTab == BrowseTab.FAVORITES) {
            internalState.update { it.copy(isLoading = false, errorResId = null, errorDetail = null) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            internalState.update { it.copy(isLoading = true, errorResId = null, errorDetail = null) }
            runCatching {
                when {
                    snapshot.selectedTab == BrowseTab.CATEGORIES && snapshot.selectedCategory == null -> {
                        val categories = getCategories()
                        internalState.update {
                            it.copy(isLoading = false, categories = categories, stations = emptyList())
                        }
                    }
                    snapshot.selectedTab == BrowseTab.WORLD && snapshot.selectedCountry == null -> {
                        val countries = getCountries()
                        internalState.update {
                            it.copy(isLoading = false, countries = countries, stations = emptyList())
                        }
                    }
                    snapshot.selectedTab == BrowseTab.PODCAST && snapshot.selectedPodcast == null -> {
                        val chart: PodcastChart = getPodcastShows(deviceRegion())
                        internalState.update {
                            it.copy(
                                isLoading = false,
                                podcasts = chart.shows,
                                podcastUsFallback = chart.usedUsFallback,
                                stations = emptyList(),
                            )
                        }
                    }
                    snapshot.selectedTab == BrowseTab.PODCAST && snapshot.selectedPodcast != null -> {
                        val region = if (snapshot.podcastUsFallback) "us" else deviceRegion()
                        val episodes = getPodcastEpisodes(snapshot.selectedPodcast, region)
                        internalState.update { it.copy(isLoading = false, stations = episodes) }
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
                        )
                        internalState.update { it.copy(isLoading = false, stations = stations) }
                    }
                }
            }.onFailure { error ->
                internalState.update {
                    it.copy(
                        isLoading = false,
                        errorResId = R.string.load_failed,
                        errorDetail = error.message?.takeIf { message -> message.isNotBlank() },
                        podcastUsFallback = false,
                    )
                }
            }
        }
    }

    fun onStationClick(station: RadioStation) {
        viewModelScope.launch {
            internalState.update { it.copy(errorResId = null, errorDetail = null) }
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
        ) as T
    }
}
