package com.apsmkimo.go2radiolive.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.apsmkimo.go2radiolive.BuildConfig
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * App-open ad shown on cold start (only if a load finishes within
 * [AppOpenPolicy.COLD_START_TIMEOUT_MS] after consent and Mobile Ads init) and when
 * the app returns to the foreground. Never shown over listening, the consent form,
 * or another full-screen ad. A loaded ad expires after four hours, and two shows
 * are at least four hours apart.
 */
object AppOpenAds : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {
    private const val PREFS = "go2radio_app_open"
    private const val KEY_LAST_SHOWN = "last_shown_ms"

    private val main = Handler(Looper.getMainLooper())
    private lateinit var app: Application

    private var currentActivity: Activity? = null
    private var appOpenAd: AppOpenAd? = null
    private var loadedAtMs: Long = 0L
    private var lastShownMs: Long = 0L
    private var isLoading = false
    private var isShowing = false
    private var installed = false
    private var coldStartStarted = false
    private var coldStartResolved = false
    private var coldStartTimeout: Runnable? = null

    fun install(application: Application) {
        if (installed) return
        installed = true
        app = application
        lastShownMs = application.getSharedPreferences(PREFS, Application.MODE_PRIVATE)
            .getLong(KEY_LAST_SHOWN, 0L)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        AdsConsent.setAdsReadyListener { onAdsReady() }
    }

    fun onAdsReady() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            handleAdsReady()
        } else {
            main.post { handleAdsReady() }
        }
    }

    private fun handleAdsReady() {
        if (!AdsConsent.canRequestAds.value || AdsConsent.consentFormVisible.value) return
        if (!coldStartStarted) {
            startColdStart()
        } else if (appOpenAd == null && !isLoading) {
            loadAd()
        }
    }

    private fun startColdStart() {
        coldStartStarted = true
        val timeout = Runnable {
            if (coldStartResolved) return@Runnable
            coldStartResolved = true
            discardIfExpired()
            if (appOpenAd != null && !audioActive()) {
                maybeShow()
            }
        }
        coldStartTimeout = timeout
        main.postDelayed(timeout, AppOpenPolicy.COLD_START_TIMEOUT_MS)
        loadAd()
    }

    override fun onStart(owner: LifecycleOwner) {
        if (!coldStartResolved) return
        if (!AdsConsent.canRequestAds.value || AdsConsent.consentFormVisible.value) return
        discardIfExpired()
        if (appOpenAd != null) {
            maybeShow()
        } else {
            loadAd()
        }
    }

    private fun loadAd() {
        if (!installed || isLoading || appOpenAd != null || isShowing) return
        if (!AdsConsent.canRequestAds.value) return
        isLoading = true
        AppOpenAd.load(
            app,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    main.post { handleLoaded(ad) }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    main.post { isLoading = false }
                }
            },
        )
    }

    private fun handleLoaded(ad: AppOpenAd) {
        isLoading = false
        appOpenAd = ad
        loadedAtMs = System.currentTimeMillis()
        if (!coldStartStarted || coldStartResolved) return
        if (playbackUnknown()) return
        coldStartTimeout?.let { main.removeCallbacks(it) }
        coldStartResolved = true
        if (!audioActive()) maybeShow()
    }

    private fun discardIfExpired() {
        if (appOpenAd == null) return
        if (AppOpenPolicy.isAdExpired(System.currentTimeMillis(), loadedAtMs)) {
            appOpenAd = null
            loadedAtMs = 0L
        }
    }

    private fun maybeShow() {
        val ad = appOpenAd ?: return
        val activity = currentActivity ?: return
        if (activity.isFinishing || activity.isDestroyed) return
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) return
        val now = System.currentTimeMillis()
        if (!AppOpenPolicy.canShowAppOpen(
                nowMs = now,
                lastShownMs = lastShownMs,
                adLoadedAtMs = loadedAtMs,
                audioActive = audioActive(),
                consentFormVisible = AdsConsent.consentFormVisible.value,
                fullScreenAdVisible = isShowing,
            )
        ) {
            return
        }
        isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                lastShownMs = System.currentTimeMillis()
                app.getSharedPreferences(PREFS, Application.MODE_PRIVATE)
                    .edit()
                    .putLong(KEY_LAST_SHOWN, lastShownMs)
                    .apply()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                appOpenAd = null
                loadedAtMs = 0L
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                isShowing = false
                appOpenAd = null
                loadedAtMs = 0L
                loadAd()
            }
        }
        val shown = runCatching { ad.show(activity) }.isSuccess
        if (!shown) {
            isShowing = false
            appOpenAd = null
            loadedAtMs = 0L
        }
    }

    /** Skip while radio or podcast audio is playing or buffering, and until playback state is known. */
    private fun audioActive(): Boolean {
        val radioApp = app as? Go2RadioApp ?: return true
        return radioApp.isAudioActive()
    }

    private fun playbackUnknown(): Boolean {
        val radioApp = app as? Go2RadioApp ?: return true
        return !radioApp.isPlaybackObserved()
    }

    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
}
