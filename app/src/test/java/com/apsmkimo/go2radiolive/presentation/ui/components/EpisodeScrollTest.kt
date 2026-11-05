package com.apsmkimo.go2radiolive.presentation.ui.components

import com.apsmkimo.go2radiolive.domain.model.RadioStation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeScrollTest {
    @Test
    fun appendingOlderEpisodesLeavesAScrolledRowInPlace() {
        val previous = rows(1, 100)
        val next = rows(1, 250)
        assertNull(
            episodeScrollAfterMerge(
                previous = previous,
                next = next,
                firstVisibleIndex = 20,
                firstVisibleOffset = 12,
                pinnedToTop = false,
            ),
        )
    }

    @Test
    fun prependingKeepsTheSameEpisodeOnScreen() {
        val previous = rows(10, 20)
        val next = rows(1, 20)
        val anchor = episodeScrollAfterMerge(
            previous = previous,
            next = next,
            firstVisibleIndex = 4,
            firstVisibleOffset = 8,
            pinnedToTop = false,
        )
        assertEquals(EpisodeScrollAnchor(index = 13, offset = 8), anchor)
    }

    @Test
    fun stayingAtTheTopDoesNotAnchorSoEpisodeOneCanArriveThere() {
        val previous = rows(200, 100)
        val next = rows(1, 300)
        assertNull(
            episodeScrollAfterMerge(
                previous = previous,
                next = next,
                firstVisibleIndex = 0,
                firstVisibleOffset = 0,
                pinnedToTop = true,
            ),
        )
    }

    private fun rows(start: Int, count: Int): List<RadioStation> {
        return List(count) { offset ->
            val number = start + offset
            RadioStation(
                stationUuid = "podcast:$number",
                name = "Episode $number",
                streamUrl = "https://cdn.example/$number.mp3",
                directStream = true,
            )
        }
    }
}
