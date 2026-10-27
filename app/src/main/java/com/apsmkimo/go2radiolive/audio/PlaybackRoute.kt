package com.apsmkimo.go2radiolive.audio

import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build

/**
 * Decides whether the media output is only the phone's built-in speaker.
 * Type numbers are the platform constants, written out so unit tests do not
 * depend on an Android stub that zeroes them.
 */
internal object PlaybackRoute {
    const val TYPE_BUILTIN_EARPIECE = 1
    const val TYPE_BUILTIN_SPEAKER = 2
    const val TYPE_WIRED_HEADSET = 3
    const val TYPE_WIRED_HEADPHONES = 4
    const val TYPE_BLUETOOTH_SCO = 7
    const val TYPE_BLUETOOTH_A2DP = 8
    const val TYPE_HDMI = 9
    const val TYPE_USB_DEVICE = 11
    const val TYPE_USB_HEADSET = 22
    const val TYPE_HEARING_AID = 23
    const val TYPE_BLE_HEADSET = 26
    const val TYPE_BLE_SPEAKER = 27

    private val headphoneTypes = intArrayOf(
        TYPE_WIRED_HEADSET,
        TYPE_WIRED_HEADPHONES,
        TYPE_BLUETOOTH_SCO,
        TYPE_BLUETOOTH_A2DP,
        TYPE_USB_HEADSET,
        TYPE_HEARING_AID,
        TYPE_BLE_HEADSET,
    )

    fun hasHeadphones(types: Collection<Int>): Boolean {
        return types.any { type -> headphoneTypes.any { it == type } }
    }

    /**
     * True only when every reported device is the built-in speaker or earpiece.
     * An empty list is unknown, not a speaker, so the UI does not warn.
     */
    fun isBuiltinSpeakerOnly(types: Collection<Int>): Boolean {
        if (types.isEmpty() || hasHeadphones(types)) return false
        if (types.any { it == TYPE_HDMI || it == TYPE_USB_DEVICE || it == TYPE_BLE_SPEAKER }) {
            return false
        }
        return types.all { it == TYPE_BUILTIN_SPEAKER || it == TYPE_BUILTIN_EARPIECE }
    }

    fun isBuiltinSpeakerOnly(audioManager: AudioManager): Boolean {
        val types = mediaOutputTypes(audioManager) ?: return false
        return isBuiltinSpeakerOnly(types)
    }

    private fun mediaOutputTypes(audioManager: AudioManager): List<Int>? {
        // SMCPKG_SUPPORT>>>Cursor116
        if (Build.VERSION.SDK_INT < 31) return null
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        return audioManager.getAudioDevicesForAttributes(attributes).map { it.type }
        // SMCPKG_SUPPORT<<<Cursor116
    }
}
