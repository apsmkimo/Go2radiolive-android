package com.apsmkimo.go2radiolive.presentation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.apsmkimo.go2radiolive.presentation.ui.home.HomeScreen
import com.apsmkimo.go2radiolive.presentation.ui.settings.SettingsScreen

@Composable
fun Go2RadioRoot() {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
    } else {
        HomeScreen(onOpenSettings = { showSettings = true })
    }
}
