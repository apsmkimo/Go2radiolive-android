package com.apsmkimo.go2radiolive.presentation.ui.components

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.ads.AdsConsent
import com.apsmkimo.go2radiolive.ads.NativeAdPool
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * One preloaded native ad, drawn like a list row. The row is absent when
 * consent disallows ads or the load failed, so the list keeps scrolling.
 * Clicks stay on the ad assets. The "Ad" label is not an ad asset.
 */
@Composable
fun NativeAdSlot(modifier: Modifier = Modifier) {
    val allowed by AdsConsent.canRequestAds.collectAsStateWithLifecycle()
    val available by NativeAdPool.available.collectAsStateWithLifecycle()
    val held = remember { mutableStateOf<NativeAd?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            val current = held.value
            held.value = null
            current?.let { runCatching { it.destroy() } }
        }
    }
    LaunchedEffect(allowed, available) {
        if (!allowed) {
            val current = held.value
            held.value = null
            current?.let { runCatching { it.destroy() } }
            return@LaunchedEffect
        }
        if (held.value == null) {
            held.value = NativeAdPool.acquire()
        }
    }
    val ad = held.value
    if (!allowed || ad == null) return

    val surface = MaterialTheme.colorScheme.surfaceVariant.toArgb()
    val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
    val muted = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val badge = MaterialTheme.colorScheme.primary.toArgb()
    val onBadge = MaterialTheme.colorScheme.onPrimary.toArgb()
    val ctaColor = MaterialTheme.colorScheme.primary.toArgb()

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        factory = { context -> createNativeAdView(context) },
        update = { view ->
            val widgets = view.tag as NativeAdWidgets
            widgets.root.setBackgroundColor(surface)
            widgets.headline.setTextColor(onSurface)
            widgets.body.setTextColor(muted)
            widgets.callToAction.setTextColor(ctaColor)
            widgets.badge.setTextColor(onBadge)
            (widgets.badge.background as? GradientDrawable)?.setColor(badge)
            if (widgets.bound !== ad) {
                bindNativeAd(widgets, ad)
            }
        },
    )
}

private class NativeAdWidgets(
    val root: NativeAdView,
    val headline: TextView,
    val body: TextView,
    val callToAction: TextView,
    val icon: ImageView,
    val badge: TextView,
) {
    var bound: NativeAd? = null
}

private fun createNativeAdView(context: android.content.Context): NativeAdView {
    val density = context.resources.displayMetrics.density
    fun dp(value: Int) = (value * density).toInt()
    val adView = NativeAdView(context)
    val row = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(dp(12), dp(12), dp(28), dp(12))
        gravity = Gravity.CENTER_VERTICAL
    }
    val icon = ImageView(context).apply {
        layoutParams = LinearLayout.LayoutParams(dp(52), dp(52))
        scaleType = ImageView.ScaleType.CENTER_CROP
    }
    val textCol = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginStart = dp(12)
        }
    }
    val badge = TextView(context).apply {
        text = context.getString(R.string.ad_label)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply {
            cornerRadius = dp(4).toFloat()
        }
        isClickable = false
    }
    val headline = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setTypeface(typeface, Typeface.BOLD)
        maxLines = 1
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    val body = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        maxLines = 2
        ellipsize = android.text.TextUtils.TruncateAt.END
    }
    val cta = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        setTypeface(typeface, Typeface.BOLD)
        setPadding(0, dp(4), 0, 0)
        maxLines = 1
    }
    textCol.addView(badge)
    textCol.addView(headline)
    textCol.addView(body)
    textCol.addView(cta)
    row.addView(icon)
    row.addView(textCol)
    adView.addView(
        row,
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ),
    )
    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = cta
    adView.iconView = icon
    adView.tag = NativeAdWidgets(adView, headline, body, cta, icon, badge)
    adView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
    return adView
}

private fun bindNativeAd(widgets: NativeAdWidgets, ad: NativeAd) {
    widgets.headline.text = ad.headline.orEmpty()
    val body = ad.body
    widgets.body.visibility = if (body.isNullOrBlank()) View.GONE else View.VISIBLE
    widgets.body.text = body.orEmpty()
    val cta = ad.callToAction
    widgets.callToAction.visibility = if (cta.isNullOrBlank()) View.GONE else View.VISIBLE
    widgets.callToAction.text = cta.orEmpty()
    val icon = ad.icon?.drawable
    widgets.icon.visibility = if (icon == null) View.GONE else View.VISIBLE
    widgets.icon.setImageDrawable(icon)
    widgets.badge.visibility = View.VISIBLE
    widgets.root.setNativeAd(ad)
    widgets.bound = ad
}
