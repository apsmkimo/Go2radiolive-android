package com.apsmkimo.go2radiolive.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SMCPKG_SUPPORT>>>Cursor117
class PodcastTextTest {
    @Test
    fun htmlTagsAndEntitiesBecomePlainText() {
        val raw = "<p>Hello &amp; welcome</p><p><b>Second line</b></p>"
        val plain = plainPodcastText(raw)
        assertEquals("Hello & welcome\nSecond line", plain)
        assertFalse(plain.contains('<'))
    }

    @Test
    fun escapedTagsAreStrippedAndTheEndingIsKept() {
        val tail = "this ending must stay visible"
        val raw = "&lt;p&gt;Opening&lt;/p&gt;&lt;p&gt;$tail&lt;/p&gt;"
        val plain = plainPodcastText(raw)
        assertFalse(plain.contains('<'))
        assertTrue(plain.endsWith(tail))
        assertTrue(plain.contains("Opening"))
    }

    @Test
    fun scriptsAreDropped() {
        val plain = plainPodcastText("<script>alert(1)</script><p>Notes</p>")
        assertEquals("Notes", plain)
    }

    @Test
    fun cjkAndJapaneseNamesStayIntact() {
        val traditional = "語音增強與很長的節目名稱"
        val simplified = "语音增强与很长的节目名称"
        val japanese = "ポッドキャストの長いタイトル"
        assertEquals(traditional, plainPodcastText(traditional))
        assertEquals(simplified, plainPodcastText(simplified))
        assertEquals(japanese, plainPodcastText(japanese))
    }

    @Test
    fun fullerShowNotesWinOverAShortBlurb() {
        val short = "Ep. 1"
        val full = "Ep. 1\n" + "詳細內容。".repeat(40)
        val picked = pickEpisodeSummary(description = short, summary = "", encoded = "<p>$full</p>")
        assertTrue(picked.length > short.length)
        assertTrue(picked.contains("詳細內容"))
        assertFalse(picked.contains('<'))
    }
}
// SMCPKG_SUPPORT<<<Cursor117
