package com.apsmkimo.go2radiolive.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.apsmkimo.go2radiolive.BuildConfig
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google UMP consent gathered on each launch. Mobile Ads starts only after
 * [ConsentInformation.canRequestAds] is true.
 * Debug builds may attach [ConsentDebugSettings] with no forced geography.
 * Release never sets a debug geography.
 */
object AdsConsent {
    private val main = Handler(Looper.getMainLooper())
    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    // SMCPKG_SUPPORT>>>Cursor091
    private val _consentFormVisible = MutableStateFlow(false)
    val consentFormVisible: StateFlow<Boolean> = _consentFormVisible.asStateFlow()

    private var consentUpdateSettled = false
    private var adsReadyListener: (() -> Unit)? = null

    fun setAdsReadyListener(listener: () -> Unit) {
        adsReadyListener = listener
    }
    // SMCPKG_SUPPORT<<<Cursor091

    private val initLock = Any()
    private var mobileAdsStarted = false

    fun request(activity: Activity) {
        // SMCPKG_SUPPORT>>>Cursor113
        // runCatching { requestConsent(activity) }
        if (AdsEntitlement.removed.value) {
            suppressForEntitlement()
            return
        }
        runCatching { requestConsent(activity) }
        // SMCPKG_SUPPORT<<<Cursor113
    }

    // SMCPKG_SUPPORT>>>Cursor113
    fun onEntitlementChanged(removed: Boolean, activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return
        if (removed) {
            suppressForEntitlement()
        } else {
            request(activity)
        }
    }

    fun suppressForEntitlement() {
        _canRequestAds.value = false
        _privacyOptionsRequired.value = false
        _consentFormVisible.value = false
        NativeAdPool.clear()
        AppOpenAds.onAdsRemoved()
    }
    // SMCPKG_SUPPORT<<<Cursor113

    private fun requestConsent(activity: Activity) {
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .apply { attachDebugSettings(activity) }
            .build()
        val consentInfo = UserMessagingPlatform.getConsentInformation(activity)
        consentInfo.requestConsentInfoUpdate(
            activity,
            params,
            {
                // SMCPKG_SUPPORT>>>Cursor113
                if (AdsEntitlement.removed.value) {
                    suppressForEntitlement()
                    return@requestConsentInfoUpdate
                }
                // SMCPKG_SUPPORT<<<Cursor113
                // SMCPKG_SUPPORT>>>Cursor091
                _consentFormVisible.value = true
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    _consentFormVisible.value = false
                    consentUpdateSettled = true
                    publish(activity)
                }
                // UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                //     publish(activity)
                // }
                // SMCPKG_SUPPORT<<<Cursor091
            },
            {
                // SMCPKG_SUPPORT>>>Cursor091
                consentUpdateSettled = true
                // SMCPKG_SUPPORT<<<Cursor091
                publish(activity)
            },
        )
        publish(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        // SMCPKG_SUPPORT>>>Cursor091
        _consentFormVisible.value = true
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            _consentFormVisible.value = false
            publish(activity)
        }
        // UserMessagingPlatform.showPrivacyOptionsForm(activity) {
        //     publish(activity)
        // }
        // SMCPKG_SUPPORT<<<Cursor091
    }

    private fun ConsentRequestParameters.Builder.attachDebugSettings(activity: Activity) {
        // SMCPKG_SUPPORT>>>Cursor084
        if (!BuildConfig.DEBUG) return
        // No DebugGeography. Forcing EEA or a regulated region must not ship.
        setConsentDebugSettings(ConsentDebugSettings.Builder(activity).build())
        // SMCPKG_SUPPORT<<<Cursor084
    }

    private fun publish(context: Context) {
        // SMCPKG_SUPPORT>>>Cursor113
        if (AdsEntitlement.removed.value) {
            suppressForEntitlement()
            return
        }
        // SMCPKG_SUPPORT<<<Cursor113
        val info = UserMessagingPlatform.getConsentInformation(context)
        val allowed = info.canRequestAds()
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (!allowed) {
            _canRequestAds.value = false
            NativeAdPool.clear()
            return
        }
        startMobileAds(context.applicationContext)
    }

    private fun startMobileAds(context: Context) {
        // SMCPKG_SUPPORT>>>Cursor113
        if (AdsEntitlement.removed.value) {
            suppressForEntitlement()
            return
        }
        // SMCPKG_SUPPORT<<<Cursor113
        val alreadyStarted = synchronized(initLock) {
            if (mobileAdsStarted) {
                true
            } else {
                mobileAdsStarted = true
                false
            }
        }
        if (alreadyStarted) {
            applyAdsGate(context)
            return
        }
        MobileAds.initialize(context) {
            main.post { applyAdsGate(context) }
        }
    }

    private fun applyAdsGate(context: Context) {
        val allowed = UserMessagingPlatform.getConsentInformation(context).canRequestAds()
        _canRequestAds.value = allowed
        if (allowed) NativeAdPool.preload(context) else NativeAdPool.clear()
        // SMCPKG_SUPPORT>>>Cursor091
        if (allowed && consentUpdateSettled && !_consentFormVisible.value) {
            adsReadyListener?.invoke()
        }
        // SMCPKG_SUPPORT<<<Cursor091
    }
}
