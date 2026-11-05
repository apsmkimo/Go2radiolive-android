package com.apsmkimo.go2radiolive.presentation.player

// SMCPKG_SUPPORT>>>Cursor123
import com.apsmkimo.go2radiolive.domain.model.RadioStation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the playing episode sits in its show's newest-first list.
 * "Newer" is the row above (index - 1); "older" is the row below (index + 1).
 */
data class EpisodeNav(
    val currentId: String? = null,
    val hasNewer: Boolean = false,
    val hasOlder: Boolean = false,
)

internal object EpisodeNavigation {
    fun indexOf(episodes: List<RadioStation>, currentId: String?): Int {
        if (currentId.isNullOrBlank()) return -1
        return episodes.indexOfFirst { it.stationUuid == currentId }
    }

    fun nav(episodes: List<RadioStation>, currentId: String?): EpisodeNav {
        val index = indexOf(episodes, currentId)
        if (index < 0) return EpisodeNav()
        return EpisodeNav(
            currentId = currentId,
            hasNewer = index > 0,
            hasOlder = index < episodes.lastIndex,
        )
    }

    /** Next = newer = index - 1 in the newest-first list. */
    fun newer(episodes: List<RadioStation>, currentId: String?): RadioStation? {
        val index = indexOf(episodes, currentId)
        return if (index > 0) episodes[index - 1] else null
    }

    /** Previous = older = index + 1 in the newest-first list. */
    fun older(episodes: List<RadioStation>, currentId: String?): RadioStation? {
        val index = indexOf(episodes, currentId)
        return if (index >= 0 && index < episodes.lastIndex) episodes[index + 1] else null
    }
}

/**
 * Process-wide bridge between [RadioPlayerController] (which owns the episode list)
 * and the media session player, so the notification / lock screen prev-next buttons
 * move through episodes in the same direction as the in-app buttons.
 */
object EpisodeQueue {
    private val _nav = MutableStateFlow(EpisodeNav())
    val nav: StateFlow<EpisodeNav> = _nav.asStateFlow()

    /** Called with true for newer (next), false for older (previous). Main thread only. */
    @Volatile
    var handler: ((newer: Boolean) -> Unit)? = null

    fun publish(nav: EpisodeNav) {
        _nav.value = nav
    }

    fun request(newer: Boolean) {
        handler?.invoke(newer)
    }
}
// SMCPKG_SUPPORT<<<Cursor123
