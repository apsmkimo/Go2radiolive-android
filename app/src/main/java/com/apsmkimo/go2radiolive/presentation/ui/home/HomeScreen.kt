package com.apsmkimo.go2radiolive.presentation.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.StarOutline
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.BrowseTab
import com.apsmkimo.go2radiolive.presentation.ui.components.AdMobBanner
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
fun HomeScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as Go2RadioApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(app.container))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val tabs = listOf(
        TabSpec(BrowseTab.LOCAL, R.string.tab_local, Icons.Outlined.Home),
        TabSpec(BrowseTab.WORLD, R.string.tab_world, Icons.Outlined.Public),
        TabSpec(BrowseTab.RESERVED, R.string.tab_reserved, Icons.Outlined.StarOutline),
        TabSpec(BrowseTab.CATEGORIES, R.string.tab_categories, Icons.Outlined.Category),
        TabSpec(BrowseTab.FAVORITES, R.string.tab_favorites, Icons.Outlined.Favorite),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = state.selectedCategory
                        ?.takeIf { state.selectedTab == BrowseTab.CATEGORIES }
                        ?.name
                        ?: stringResource(R.string.app_name)
                    Text(title)
                },
                navigationIcon = {
                    if (state.selectedTab == BrowseTab.CATEGORIES && state.selectedCategory != null) {
                        IconButton(onClick = viewModel::onClearCategory) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                },
            )
        },
        bottomBar = {
            Column {
                MiniPlayer(
                    playback = state.playback,
                    onTogglePlayPause = viewModel::onTogglePlayPause,
                )
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
            if (state.showingCategories) {
                CategoryList(
                    categories = state.visibleCategories,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    onCategoryClick = viewModel::onCategorySelected,
                    onRetry = viewModel::refresh,
                )
            } else {
                val emptyText = when (state.selectedTab) {
                    BrowseTab.FAVORITES -> stringResource(R.string.empty_favorites)
                    else -> stringResource(R.string.empty_stations)
                }
                StationList(
                    stations = state.visibleStations,
                    favoriteIds = state.favoriteIds,
                    isLoading = state.isLoading,
                    errorMessage = state.errorMessage,
                    emptyText = emptyText,
                    onStationClick = viewModel::onStationClick,
                    onToggleFavorite = viewModel::onToggleFavorite,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}
