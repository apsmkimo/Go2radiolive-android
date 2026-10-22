package com.apsmkimo.go2radiolive.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.ads.AdsConsent
import com.apsmkimo.go2radiolive.ads.AdsEntitlement
import com.apsmkimo.go2radiolive.presentation.ui.Go2RadioRoot
import com.apsmkimo.go2radiolive.presentation.ui.theme.Go2RadioTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        volumeControlStream = AudioManager.STREAM_MUSIC
        requestNotificationPermissionIfNeeded()
        // SMCPKG_SUPPORT>>>Cursor113
        // AdsConsent.request(this)
        lifecycleScope.launch {
            AdsEntitlement.removed.collect { removed ->
                AdsConsent.onEntitlementChanged(removed, this@MainActivity)
            }
        }
        // SMCPKG_SUPPORT<<<Cursor113
        setContent {
            Go2RadioTheme {
                // SMCPKG_SUPPORT>>>Cursor014
                // HomeScreen()
                Go2RadioRoot()
                // SMCPKG_SUPPORT<<<Cursor014
            }
        }
    }

    // SMCPKG_SUPPORT>>>Cursor113
    override fun onResume() {
        super.onResume()
        (application as Go2RadioApp).container.billingManager.refreshPurchases()
    }
    // SMCPKG_SUPPORT<<<Cursor113

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
