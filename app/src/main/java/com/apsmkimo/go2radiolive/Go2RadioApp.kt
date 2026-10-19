package com.apsmkimo.go2radiolive

import android.app.Application
import com.apsmkimo.go2radiolive.ads.AppOpenAds
import com.apsmkimo.go2radiolive.di.AppContainer

class Go2RadioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.playerController.initialize()
        // SMCPKG_SUPPORT>>>Cursor091
        AppOpenAds.install(this)
        // SMCPKG_SUPPORT<<<Cursor091
        // SMCPKG_SUPPORT>>>Cursor083
        // MobileAds.initialize waits until UMP says canRequestAds(). See AdsConsent.
        // applicationScope.launch {
        //     runCatching { MobileAds.initialize(this@Go2RadioApp) }
        // }
        // SMCPKG_SUPPORT<<<Cursor083
    }

    // SMCPKG_SUPPORT>>>Cursor091
    /** True while playback is unknown, or radio/podcast audio is playing or buffering. */
    fun isAudioActive(): Boolean {
        if (!::container.isInitialized) return true
        val player = container.playerController
        if (!player.playbackObserved) return true
        val playback = player.playbackState.value
        return playback.isPlaying || playback.isBuffering
    }

    fun isPlaybackObserved(): Boolean {
        if (!::container.isInitialized) return false
        return container.playerController.playbackObserved
    }
    // SMCPKG_SUPPORT<<<Cursor091
}
