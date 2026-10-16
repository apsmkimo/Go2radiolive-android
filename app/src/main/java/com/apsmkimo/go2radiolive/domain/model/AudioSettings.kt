package com.apsmkimo.go2radiolive.domain.model

/**
 * User tone controls shared by the settings screen, the mini-player panel, and the playback DSP.
 * Gains are decibels. Balance is -1 (full left) .. +1 (full right).
 */
data class AudioSettings(
    val showPanelButton: Boolean = false,
    val bassDb: Float = 0f,
    val lowDb: Float = 0f,
    val midDb: Float = 0f,
    val highDb: Float = 0f,
    val balance: Float = 0f,
    val voiceEnhancement: Boolean = false,
    val virtualSurround: Boolean = false,
) {
    fun sanitized(): AudioSettings = copy(
        // SMCPKG_SUPPORT>>>Cursor034
        // bassDb = bassDb.coerceIn(BAND_DB_MIN, BAND_DB_MAX),
        bassDb = bassDb.coerceIn(BASS_DB_MIN, BASS_DB_MAX),
        // SMCPKG_SUPPORT<<<Cursor034
        lowDb = lowDb.coerceIn(BAND_DB_MIN, BAND_DB_MAX),
        midDb = midDb.coerceIn(BAND_DB_MIN, BAND_DB_MAX),
        highDb = highDb.coerceIn(BAND_DB_MIN, BAND_DB_MAX),
        balance = balance.coerceIn(BALANCE_MIN, BALANCE_MAX),
    )

    companion object {
        const val BAND_DB_MIN = -12f
        const val BAND_DB_MAX = 12f
        // SMCPKG_SUPPORT>>>Cursor035
        const val BASS_DB_MIN = -12f
        const val BASS_DB_MAX = 15f
        // SMCPKG_SUPPORT<<<Cursor035
        const val BALANCE_MIN = -1f
        const val BALANCE_MAX = 1f
    }
}
