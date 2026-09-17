package com.apsmkimo.go2radiolive.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.apsmkimo.go2radiolive.di.AppContainer
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.domain.model.PlaybackState
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import com.apsmkimo.go2radiolive.domain.model.StationCategory
import com.apsmkimo.go2radiolive.domain.usecase.GetCategoriesUseCase
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
    val favoriteIds: Set<String> = emptySet(),
    val favoriteStations: List<RadioStation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
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
        get() {
            val query = searchQuery.trim()
            return if (query.isEmpty()) {
                categories
            } else {
                categories.filter { it.name.contains(query, ignoreCase = true) }
            }
        }

    val showingCategories: Boolean
        get() = selectedTab == BrowseTab.CATEGORIES && selectedCategory == null
}

class HomeViewModel(
    private val getStations: GetStationsUseCase,
    private val getCategories: GetCategoriesUseCase,
    observeFavorites: ObserveFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val resolveStreamUrl: ResolveStreamUrlUseCase,
    private val playerController: RadioPlayerController,
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
            errorMessage = ui.errorMessage ?: playback.errorMessage,
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
                searchQuery = "",
                errorMessage = null,
            )
        }
        refresh()
    }

    fun onSearchQueryChange(query: String) {
        internalState.update { it.copy(searchQuery = query) }
    }

    fun onCategorySelected(category: StationCategory) {
        internalState.update {
            it.copy(selectedCategory = category, searchQuery = "", errorMessage = null)
        }
        refresh()
    }

    fun onClearCategory() {
        internalState.update {
            it.copy(selectedCategory = null, stations = emptyList(), searchQuery = "")
        }
        refresh()
    }

    fun refresh() {
        val snapshot = internalState.value
        if (snapshot.selectedTab == BrowseTab.FAVORITES) {
            internalState.update { it.copy(isLoading = false, errorMessage = null) }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            internalState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                if (snapshot.selectedTab == BrowseTab.CATEGORIES && snapshot.selectedCategory == null) {
                    internalState.update {
                        it.copy(isLoading = false, categories = getCategories(), stations = emptyList())
                    }
                } else {
                    val stations = getStations(snapshot.selectedTab, snapshot.selectedCategory?.name)
                    internalState.update { it.copy(isLoading = false, stations = stations) }
                }
            }.onFailure { error ->
                internalState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "載入失敗，請稍後再試",
                    )
                }
            }
        }
    }

    fun onStationClick(station: RadioStation) {
        viewModelScope.launch {
            internalState.update { it.copy(errorMessage = null) }
            val streamUrl = runCatching { resolveStreamUrl(station) }
                .getOrDefault(station.streamUrl)
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
            observeFavorites = container.observeFavoritesUseCase,
            toggleFavorite = container.toggleFavoriteUseCase,
            resolveStreamUrl = container.resolveStreamUrlUseCase,
            playerController = container.playerController,
        ) as T
    }
}
