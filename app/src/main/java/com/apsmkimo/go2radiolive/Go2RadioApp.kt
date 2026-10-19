package com.apsmkimo.go2radiolive

import android.app.Application
import com.apsmkimo.go2radiolive.di.AppContainer

class Go2RadioApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.playerController.initialize()
        // SMCPKG_SUPPORT>>>Cursor083
        // MobileAds.initialize waits until UMP says canRequestAds(). See AdsConsent.
        // applicationScope.launch {
        //     runCatching { MobileAds.initialize(this@Go2RadioApp) }
        // }
        // SMCPKG_SUPPORT<<<Cursor083
    }
}
