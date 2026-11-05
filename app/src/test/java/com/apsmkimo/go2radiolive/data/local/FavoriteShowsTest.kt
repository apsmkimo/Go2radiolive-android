package com.apsmkimo.go2radiolive.data.local

// SMCPKG_SUPPORT>>>Cursor123
import com.apsmkimo.go2radiolive.domain.model.PodcastShow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FavoriteShowsTest {
    private val a = PodcastShow(id = "1", name = "A", author = "x", feedUrl = "https://a/feed")
    private val b = PodcastShow(id = "2", name = "B", author = "y", feedUrl = "")

    @Test
    fun addsNewestFirstAndKeepsFeedUrl() {
        val saved = toggledFavoriteShows(toggledFavoriteShows(emptyList(), a), b)
        assertEquals(listOf("2", "1"), saved.map { it.id })
        assertEquals("https://a/feed", saved[1].feedUrl)
        assertNull(saved[0].feedUrl)
    }

    @Test
    fun togglingAgainRemoves() {
        val saved = toggledFavoriteShows(listOf(a, b), a.copy(name = "renamed"))
        assertEquals(listOf("2"), saved.map { it.id })
    }
}
// SMCPKG_SUPPORT<<<Cursor123
