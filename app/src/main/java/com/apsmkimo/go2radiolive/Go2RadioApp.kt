package com.apsmkimo.go2radiolive

import android.app.Application
import com.apsmkimo.go2radiolive.di.AppContainer
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class Go2RadioApp : Application() {

    lateinit var container: AppContainer
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.playerController.initialize()
        applicationScope.launch {
            runCatching { MobileAds.initialize(this@Go2RadioApp) }
        }
    }
}
