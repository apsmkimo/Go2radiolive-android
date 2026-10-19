package com.apsmkimo.go2radiolive.ads

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.apsmkimo.go2radiolive.BuildConfig
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A few preloaded native ads. Rows take one without waiting on the network,
 * and destroy it when they leave the screen. A failed load leaves the row out.
 */
internal object NativeAdPool {
    private const val POOL_SIZE = 3
    private val main = Handler(Looper.getMainLooper())
    private val ready = ArrayDeque<NativeAd>()
    private val lock = Any()
    private var inFlight = 0
    private var appContext: Context? = null

    private val _available = MutableStateFlow(0)
    val available: StateFlow<Int> = _available.asStateFlow()

    fun preload(context: Context) {
        appContext = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            refill()
        } else {
            main.post { refill() }
        }
    }

    fun acquire(): NativeAd? {
        val ad = synchronized(lock) { if (ready.isEmpty()) null else ready.removeFirst() }
        publish()
        refill()
        return ad
    }

    fun clear() {
        val leftover = synchronized(lock) {
            val copy = ready.toList()
            ready.clear()
            copy
        }
        leftover.forEach { runCatching { it.destroy() } }
        publish()
    }

    private fun refill() {
        val context = appContext ?: return
        if (!AdsConsent.canRequestAds.value) return
        while (true) {
            val shouldLoad = synchronized(lock) {
                if (ready.size + inFlight >= POOL_SIZE) {
                    false
                } else {
                    inFlight++
                    true
                }
            }
            if (!shouldLoad) return
            loadOne(context)
        }
    }

    private fun loadOne(context: Context) {
        val loader = AdLoader.Builder(context, BuildConfig.ADMOB_NATIVE_ID)
            .forNativeAd { ad ->
                val keep = synchronized(lock) {
                    inFlight = (inFlight - 1).coerceAtLeast(0)
                    if (AdsConsent.canRequestAds.value && ready.size < POOL_SIZE) {
                        ready.addLast(ad)
                        true
                    } else {
                        false
                    }
                }
                if (!keep) {
                    runCatching { ad.destroy() }
                }
                publish()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    synchronized(lock) {
                        inFlight = (inFlight - 1).coerceAtLeast(0)
                    }
                    publish()
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build(),
            )
            .build()
        loader.loadAd(AdRequest.Builder().build())
    }

    private fun publish() {
        val count = synchronized(lock) { ready.size }
        _available.value = count
    }
}
