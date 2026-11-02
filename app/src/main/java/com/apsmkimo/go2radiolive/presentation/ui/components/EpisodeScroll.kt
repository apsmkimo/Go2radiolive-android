package com.apsmkimo.go2radiolive.presentation.ui.components

import com.apsmkimo.go2radiolive.domain.model.RadioStation

// SMCPKG_SUPPORT>>>Cursor122
internal data class EpisodeScrollAnchor(
    val index: Int,
    val offset: Int,
)

/**
 * Keeps the episode the user is looking at when older or newer rows are merged in.
 * Staying at the top is intentional: newest-first keeps the latest episode there,
 * and oldest-first keeps episode 1 there once the full feed arrives.
 */
internal fun episodeScrollAfterMerge(
    previous: List<RadioStation>,
    next: List<RadioStation>,
    firstVisibleIndex: Int,
    firstVisibleOffset: Int,
    pinnedToTop: Boolean,
): EpisodeScrollAnchor? {
    if (pinnedToTop) return null
    if (previous.isEmpty() || next.isEmpty() || previous === next) return null
    val index = firstVisibleIndex.coerceAtLeast(0)
    val key = previous.getOrNull(index)?.stationUuid ?: return null
    if (next.getOrNull(index)?.stationUuid == key) return null
    val newIndex = next.indexOfFirst { it.stationUuid == key }
    if (newIndex < 0 || newIndex == index) return null
    return EpisodeScrollAnchor(newIndex, firstVisibleOffset.coerceAtLeast(0))
}
// SMCPKG_SUPPORT<<<Cursor122
