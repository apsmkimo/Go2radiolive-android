package com.apsmkimo.go2radiolive.presentation.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.go2radiolive.BuildConfig
import com.apsmkimo.go2radiolive.ads.AdsConsent
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun AdMobBanner(modifier: Modifier = Modifier) {
    val canRequestAds by AdsConsent.canRequestAds.collectAsStateWithLifecycle()
    if (!canRequestAds) return
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        val widthDp = maxWidth.value.toInt()
        if (widthDp <= 0) return@BoxWithConstraints
        key(widthDp) {
            val held = remember { mutableStateOf<AdView?>(null) }
            DisposableEffect(Unit) {
                onDispose {
                    held.value?.destroy()
                    held.value = null
                }
            }
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { context ->
                    AdView(context).apply {
                        adUnitId = BuildConfig.ADMOB_BANNER_ID
                        // SMCPKG_SUPPORT>>>Cursor085
                        // setAdSize(AdSize.BANNER)
                        setAdSize(
                            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp),
                        )
                        // SMCPKG_SUPPORT<<<Cursor085
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                        )
                        loadAd(AdRequest.Builder().build())
                        held.value = this
                    }
                },
            )
        }
    }
}
