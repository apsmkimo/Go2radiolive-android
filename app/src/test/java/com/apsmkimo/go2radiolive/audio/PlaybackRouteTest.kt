package com.apsmkimo.go2radiolive.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

// SMCPKG_SUPPORT>>>Cursor116
class PlaybackRouteTest {
    @Test
    fun speakerOrEarpieceAloneCountsAsBuiltinSpeaker() {
        assertTrue(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_BUILTIN_SPEAKER)))
        assertTrue(
            PlaybackRoute.isBuiltinSpeakerOnly(
                listOf(PlaybackRoute.TYPE_BUILTIN_SPEAKER, PlaybackRoute.TYPE_BUILTIN_EARPIECE),
            ),
        )
    }

    @Test
    fun headphonesAndUnknownRoutesAreNotASpeakerWarning() {
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(emptyList()))
        assertTrue(PlaybackRoute.hasHeadphones(listOf(PlaybackRoute.TYPE_WIRED_HEADPHONES)))
        assertFalse(
            PlaybackRoute.isBuiltinSpeakerOnly(
                listOf(PlaybackRoute.TYPE_BUILTIN_SPEAKER, PlaybackRoute.TYPE_WIRED_HEADPHONES),
            ),
        )
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_BLUETOOTH_A2DP)))
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_USB_HEADSET)))
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_BLE_HEADSET)))
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_HDMI)))
        assertFalse(PlaybackRoute.isBuiltinSpeakerOnly(listOf(PlaybackRoute.TYPE_USB_DEVICE)))
    }
}
// SMCPKG_SUPPORT<<<Cursor116
