package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateMap

// SMCPKG_SUPPORT>>>Cursor118
internal const val EPISODE_DESCRIPTION_COLLAPSED_LINES = 3

/** True when a 3-line preview cannot show the whole description, or it is already open. */
internal fun showEpisodeDescriptionToggle(hasOverflow: Boolean, expanded: Boolean): Boolean {
    return hasOverflow || expanded
}

internal fun episodeDescriptionMaxLines(expanded: Boolean): Int {
    return if (expanded) Int.MAX_VALUE else EPISODE_DESCRIPTION_COLLAPSED_LINES
}

/**
 * Which episode descriptions are open. Owned by the episode list so scrolling
 * can recycle rows without forgetting, and leaving the list drops the state.
 */
@Stable
internal class EpisodeDescriptionExpansion(
    private val expanded: SnapshotStateMap<String, Boolean> = mutableStateMapOf(),
    private val overflowing: SnapshotStateMap<String, Boolean> = mutableStateMapOf(),
) {
    fun isExpanded(episodeId: String): Boolean = expanded[episodeId] == true

    fun overflows(episodeId: String): Boolean = overflowing[episodeId] == true

    /** Remember a measured overflow so a recycled row can keep its control. */
    fun markOverflow(episodeId: String) {
        if (overflowing[episodeId] != true) overflowing[episodeId] = true
    }

    fun toggle(episodeId: String) {
        if (isExpanded(episodeId)) {
            expanded.remove(episodeId)
        } else {
            expanded[episodeId] = true
        }
    }
}
// SMCPKG_SUPPORT<<<Cursor118
