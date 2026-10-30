package com.apsmkimo.go2radiolive.presentation.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Configuration
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.go2radiolive.BuildConfig
import com.apsmkimo.go2radiolive.Go2RadioApp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.ads.AdsConsent
import com.apsmkimo.go2radiolive.ads.AdsEntitlement
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.audio.PlaybackRoute
import com.apsmkimo.go2radiolive.data.billing.RemoveAdsUiState
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import com.apsmkimo.go2radiolive.locale.AppLanguage
import com.apsmkimo.go2radiolive.locale.AppLocale
import com.apsmkimo.go2radiolive.presentation.ui.components.AudioEffectControls
import com.apsmkimo.go2radiolive.presentation.ui.safeDrawingPadding
import com.apsmkimo.go2radiolive.presentation.ui.screenContentInsets
import com.apsmkimo.go2radiolive.presentation.ui.theme.Go2RadioTheme
import com.apsmkimo.go2radiolive.presentation.ui.topSafePadding

private const val PRIVACY_POLICY_URL = "https://apsmkimo.github.io/privacy-go2radio.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings by AudioSettingsRepository.settings.collectAsStateWithLifecycle()
    val privacyOptionsRequired by AdsConsent.privacyOptionsRequired.collectAsStateWithLifecycle()
    // SMCPKG_SUPPORT>>>Cursor113
    val adsRemoved by AdsEntitlement.removed.collectAsStateWithLifecycle()
    val billingManager = (LocalContext.current.applicationContext as Go2RadioApp).container.billingManager
    val removeAds by billingManager.state.collectAsStateWithLifecycle()
    // SMCPKG_SUPPORT<<<Cursor113
    val activity = LocalContext.current.findActivity()
    val context = LocalContext.current
    // SMCPKG_SUPPORT>>>Cursor121
    var languageTag by remember { mutableStateOf(AppLocale.selectedTag()) }
    val speakerOnly = rememberBuiltinSpeakerOnly()
    // SMCPKG_SUPPORT<<<Cursor121

    // SMCPKG_SUPPORT>>>Cursor121
    // Scaffold(
    //     contentWindowInsets = screenContentInsets(),
    //     topBar = {
    //         TopAppBar(
    //             modifier = Modifier.padding(topSafePadding()),
    //             windowInsets = WindowInsets(0, 0, 0, 0),
    //             title = { Text(stringResource(R.string.settings)) },
    //             navigationIcon = {
    //                 IconButton(onClick = onBack) {
    //                     Icon(
    //                         imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
    //                         contentDescription = stringResource(R.string.back),
    //                     )
    //                 }
    //             },
    //         )
    //     },
    // ) { innerPadding ->
    //     Column(
    //         modifier = Modifier
    //             .fillMaxSize()
    //             .padding(innerPadding)
    //             .verticalScroll(rememberScrollState())
    //             .padding(bottom = 24.dp),
    //     ) {
    //         RemoveAdsCard(...)
    //         version Surface card
    //         privacy options row
    //         audio panel switch
    //         voice enhancement switch
    //         SpatialAudioSection
    //         HorizontalDivider
    //         EQ title, shared hint, AudioEffectControls
    //         HorizontalDivider
    //         LanguageSection()
    //         debug remove-ads override
    //     }
    // }
    SettingsScaffold(onBack = onBack) {
        SettingsBody(
            settings = settings,
            removeAds = removeAds,
            showPrivacyOptions = privacyOptionsRequired && !adsRemoved,
            showDebugOverride = BuildConfig.DEBUG,
            languageTag = languageTag,
            speakerOnly = speakerOnly,
            onBuy = { activity?.let(billingManager::buy) },
            onRestore = billingManager::restore,
            onPrivacyOptions = { activity?.let(AdsConsent::showPrivacyOptions) },
            onPrivacyPolicy = context::openPrivacyPolicy,
            onLanguageSelected = { tag ->
                languageTag = tag
                AppLocale.apply(tag)
            },
            onDebugOverride = billingManager::setDebugOverride,
        )
    }
    // SMCPKG_SUPPORT<<<Cursor121
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScaffold(
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    Scaffold(
        // SMCPKG_SUPPORT>>>Cursor112
        contentWindowInsets = screenContentInsets(),
        // SMCPKG_SUPPORT<<<Cursor112
        topBar = {
            TopAppBar(
                // SMCPKG_SUPPORT>>>Cursor112
                modifier = Modifier.padding(topSafePadding()),
                windowInsets = WindowInsets(0, 0, 0, 0),
                // SMCPKG_SUPPORT<<<Cursor112
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            content = { content() },
        )
    }
}

@Composable
private fun SettingsBody(
    settings: AudioSettings,
    removeAds: RemoveAdsUiState,
    showPrivacyOptions: Boolean,
    showDebugOverride: Boolean,
    languageTag: String,
    speakerOnly: Boolean,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
    onPrivacyOptions: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    onDebugOverride: (Boolean) -> Unit,
) {
    // SMCPKG_SUPPORT>>>Cursor121
    RemoveAdsCard(
        state = removeAds,
        onBuy = onBuy,
        onRestore = onRestore,
    )
    SettingsSection(title = stringResource(R.string.settings_section_audio)) {
        SettingsSwitchRow(
            title = stringResource(R.string.voice_enhancement),
            summary = stringResource(R.string.voice_enhancement_summary),
            checked = settings.voiceEnhancement,
            onCheckedChange = AudioSettingsRepository::setVoiceEnhancement,
        )
        SpatialAudioSection(settings = settings, speakerOnly = speakerOnly)
        Text(
            text = stringResource(R.string.eq_section),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 2.dp),
        )
        Text(
            text = stringResource(R.string.eq_shared_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )
        AudioEffectControls(
            settings = settings,
            onBassChange = AudioSettingsRepository::setBassDb,
            onLowChange = AudioSettingsRepository::setLowDb,
            onMidChange = AudioSettingsRepository::setMidDb,
            onHighChange = AudioSettingsRepository::setHighDb,
            onBalanceChange = AudioSettingsRepository::setBalance,
            onReset = AudioSettingsRepository::resetTone,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
    SettingsSection(title = stringResource(R.string.settings_section_general)) {
        LanguagePreferenceRow(
            languageTag = languageTag,
            onLanguageSelected = onLanguageSelected,
        )
        SettingsSwitchRow(
            title = stringResource(R.string.audio_panel_button),
            summary = stringResource(R.string.audio_panel_button_summary),
            checked = settings.showPanelButton,
            onCheckedChange = AudioSettingsRepository::setShowPanelButton,
        )
    }
    SettingsSection(title = stringResource(R.string.settings_section_about)) {
        if (showPrivacyOptions) {
            SettingsClickRow(
                title = stringResource(R.string.privacy_options),
                summary = stringResource(R.string.privacy_options_summary),
                onClick = onPrivacyOptions,
            )
        }
        SettingsClickRow(
            title = stringResource(R.string.privacy_policy),
            summary = stringResource(R.string.privacy_policy_summary),
            onClick = onPrivacyPolicy,
        )
        Text(
            text = stringResource(R.string.settings_version_title),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp),
        )
        Text(
            text = stringResource(
                R.string.settings_version_value,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp),
        )
    }
    if (showDebugOverride) {
        SettingsCard(modifier = Modifier.padding(top = 20.dp)) {
            SettingsSwitchRow(
                title = stringResource(R.string.remove_ads_debug),
                summary = stringResource(R.string.remove_ads_debug_summary),
                checked = removeAds.debugOverride,
                onCheckedChange = onDebugOverride,
            )
        }
    }
    // SMCPKG_SUPPORT<<<Cursor121
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    // SMCPKG_SUPPORT>>>Cursor121
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.4.sp,
        ),
        color = accentColor(),
        modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 18.dp, bottom = 6.dp),
    )
    SettingsCard(content = content)
    // SMCPKG_SUPPORT<<<Cursor121
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}

@Composable
private fun accentColor(): Color {
    // SMCPKG_SUPPORT>>>Cursor121
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) Color(0xFF8ED9D4) else MaterialTheme.colorScheme.primary
    // SMCPKG_SUPPORT<<<Cursor121
}

// SMCPKG_SUPPORT>>>Cursor113
@Composable
private fun RemoveAdsCard(
    state: RemoveAdsUiState,
    onBuy: () -> Unit,
    onRestore: () -> Unit,
) {
    val price = state.formattedPrice
    val showPrice = !state.entitled && state.productAvailable && !price.isNullOrBlank()
    val showUnavailable = !state.entitled && !state.pending && state.detailsSettled && !showPrice
    val canBuy = showPrice && state.billingReady && !state.pending && !state.purchaseInProgress
    // SMCPKG_SUPPORT>>>Cursor121
    if (state.entitled) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text(
                text = stringResource(R.string.remove_ads_thanks),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
        return
    }
    // SMCPKG_SUPPORT<<<Cursor121
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            // SMCPKG_SUPPORT>>>Cursor121
            // .padding(horizontal = 16.dp, vertical = 12.dp),
            .padding(horizontal = 16.dp, vertical = 8.dp),
            // SMCPKG_SUPPORT<<<Cursor121
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            // SMCPKG_SUPPORT>>>Cursor121
            // modifier = Modifier.padding(16.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            // SMCPKG_SUPPORT<<<Cursor121
        ) {
            Text(
                text = stringResource(R.string.remove_ads_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = when {
                    state.entitled -> stringResource(R.string.remove_ads_thanks)
                    state.pending -> stringResource(R.string.remove_ads_pending)
                    showPrice -> stringResource(R.string.remove_ads_price, price.orEmpty())
                    showUnavailable -> stringResource(R.string.remove_ads_unavailable)
                    else -> stringResource(R.string.remove_ads_summary)
                },
                // SMCPKG_SUPPORT>>>Cursor121
                // style = MaterialTheme.typography.bodyLarge,
                style = MaterialTheme.typography.bodyMedium,
                // SMCPKG_SUPPORT<<<Cursor121
                color = when {
                    state.entitled -> MaterialTheme.colorScheme.primary
                    showUnavailable -> MaterialTheme.colorScheme.onSurfaceVariant
                    else -> MaterialTheme.colorScheme.onSurface
                },
                // SMCPKG_SUPPORT>>>Cursor121
                // modifier = Modifier.padding(top = 4.dp),
                modifier = Modifier.padding(top = 2.dp),
                // SMCPKG_SUPPORT<<<Cursor121
            )
            if (!state.entitled && (showPrice || state.pending || showUnavailable)) {
                Text(
                    text = stringResource(R.string.remove_ads_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    // SMCPKG_SUPPORT>>>Cursor121
                    // modifier = Modifier.padding(top = 4.dp),
                    modifier = Modifier.padding(top = 2.dp),
                    // SMCPKG_SUPPORT<<<Cursor121
                )
            }
            if (!state.entitled) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        // SMCPKG_SUPPORT>>>Cursor121
                        // .padding(top = 12.dp),
                        .padding(top = 8.dp),
                        // SMCPKG_SUPPORT<<<Cursor121
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onBuy,
                        enabled = canBuy,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.remove_ads_buy),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = stringResource(R.string.remove_ads_restore),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
// SMCPKG_SUPPORT<<<Cursor113

// SMCPKG_SUPPORT>>>Cursor121
// @Composable
// private fun LanguageSection() {
//     var selected by remember { mutableStateOf(AppLocale.selectedTag()) }
//     Text(
//         text = stringResource(R.string.settings_language),
//         style = MaterialTheme.typography.titleMedium,
//         modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
//     )
//     AppLanguage.tags.forEach { tag ->
//         val title = if (tag == AppLanguage.SYSTEM) {
//             stringResource(R.string.settings_language_system)
//         } else {
//             languageName(tag)
//         }
//         SpatialModeRow(
//             title = title,
//             summary = "",
//             selected = selected == tag,
//             onSelect = {
//                 selected = tag
//                 AppLocale.apply(tag)
//             },
//         )
//     }
// }
@Composable
private fun LanguagePreferenceRow(
    languageTag: String,
    onLanguageSelected: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    SettingsValueRow(
        title = stringResource(R.string.settings_language),
        value = languageLabel(languageTag),
        onClick = { showDialog = true },
    )
    if (showDialog) {
        LanguageDialog(
            selected = languageTag,
            onDismiss = { showDialog = false },
            onSelect = { tag ->
                showDialog = false
                onLanguageSelected(tag)
            },
        )
    }
}

@Composable
private fun LanguageDialog(
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.padding(safeDrawingPadding()),
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                AppLanguage.tags.forEach { tag ->
                    SpatialModeRow(
                        title = languageLabel(tag),
                        summary = "",
                        selected = selected == tag,
                        onSelect = { onSelect(tag) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun languageLabel(tag: String): String {
    return if (tag == AppLanguage.SYSTEM) {
        stringResource(R.string.settings_language_system)
    } else {
        languageName(tag)
    }
}
// SMCPKG_SUPPORT<<<Cursor121

private fun languageName(tag: String): String = when (tag) {
    AppLanguage.ENGLISH -> "English"
    AppLanguage.TRADITIONAL -> "繁體中文"
    AppLanguage.SIMPLIFIED -> "简体中文"
    AppLanguage.JAPANESE -> "日本語"
    else -> tag
}

@Composable
private fun SpatialAudioSection(
    settings: AudioSettings,
    speakerOnly: Boolean,
) {
    // SMCPKG_SUPPORT>>>Cursor116
    // val speakerOnly = rememberBuiltinSpeakerOnly()
    // SMCPKG_SUPPORT<<<Cursor116
    // SMCPKG_SUPPORT>>>Cursor121
    // Text(
    //     text = stringResource(R.string.spatial_section),
    //     style = MaterialTheme.typography.titleMedium,
    //     modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    // )
    Text(
        text = stringResource(R.string.spatial_section),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
    )
    // SMCPKG_SUPPORT<<<Cursor121
    // SMCPKG_SUPPORT>>>Cursor116
    if (speakerOnly && settings.spatialMode != SpatialAudioMode.OFF) {
        Text(
            text = stringResource(R.string.spatial_speaker_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
    }
    // SMCPKG_SUPPORT<<<Cursor116
    SpatialModeRow(
        title = stringResource(R.string.spatial_off),
        summary = stringResource(R.string.spatial_off_summary),
        selected = settings.spatialMode == SpatialAudioMode.OFF,
        onSelect = { AudioSettingsRepository.setSpatialMode(SpatialAudioMode.OFF) },
    )
    SpatialModeRow(
        title = stringResource(R.string.spatial_natural),
        summary = stringResource(R.string.spatial_natural_summary),
        selected = settings.spatialMode == SpatialAudioMode.NATURAL,
        onSelect = { AudioSettingsRepository.setSpatialMode(SpatialAudioMode.NATURAL) },
    )
    SpatialModeRow(
        title = stringResource(R.string.spatial_surround),
        summary = stringResource(R.string.spatial_surround_summary),
        selected = settings.spatialMode == SpatialAudioMode.SURROUND,
        onSelect = { AudioSettingsRepository.setSpatialMode(SpatialAudioMode.SURROUND) },
    )
    SpatialModeRow(
        title = stringResource(R.string.spatial_orbit),
        summary = stringResource(R.string.spatial_orbit_summary),
        selected = settings.spatialMode == SpatialAudioMode.ORBIT,
        onSelect = { AudioSettingsRepository.setSpatialMode(SpatialAudioMode.ORBIT) },
    )
    if (settings.spatialMode == SpatialAudioMode.ORBIT) {
        OrbitControls(settings)
    }
}

// SMCPKG_SUPPORT>>>Cursor116
@Composable
private fun rememberBuiltinSpeakerOnly(): Boolean {
    val context = LocalContext.current
    var speakerOnly by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        if (audioManager == null) {
            onDispose { }
        } else {
            fun refresh() {
                speakerOnly = PlaybackRoute.isBuiltinSpeakerOnly(audioManager)
            }
            val callback = object : AudioDeviceCallback() {
                override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
                override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
            }
            refresh()
            audioManager.registerAudioDeviceCallback(callback, Handler(Looper.getMainLooper()))
            onDispose { audioManager.unregisterAudioDeviceCallback(callback) }
        }
    }
    return speakerOnly
}
// SMCPKG_SUPPORT<<<Cursor116

@Composable
private fun SpatialModeRow(
    title: String,
    summary: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrbitControls(settings: AudioSettings) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.orbit_speed),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.orbit_speed_value, settings.orbitPeriodSec),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.orbitPeriodSec,
            onValueChange = AudioSettingsRepository::setOrbitPeriodSec,
            valueRange = AudioSettings.ORBIT_PERIOD_MIN..AudioSettings.ORBIT_PERIOD_MAX,
        )
        Text(
            text = stringResource(R.string.orbit_direction),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        // SMCPKG_SUPPORT>>>Cursor121
        // Row(verticalAlignment = Alignment.CenterVertically) {
        //     RadioButton(
        //         selected = settings.orbitClockwise,
        //         onClick = { AudioSettingsRepository.setOrbitClockwise(true) },
        //     )
        //     Text(
        //         text = stringResource(R.string.orbit_clockwise),
        //         modifier = Modifier
        //             .clickable { AudioSettingsRepository.setOrbitClockwise(true) }
        //             .padding(end = 12.dp),
        //     )
        //     RadioButton(
        //         selected = !settings.orbitClockwise,
        //         onClick = { AudioSettingsRepository.setOrbitClockwise(false) },
        //     )
        //     Text(
        //         text = stringResource(R.string.orbit_counterclockwise),
        //         modifier = Modifier.clickable { AudioSettingsRepository.setOrbitClockwise(false) },
        //     )
        // }
        FlowRow {
            OrbitDirectionChoice(
                label = stringResource(R.string.orbit_clockwise),
                selected = settings.orbitClockwise,
                onSelect = { AudioSettingsRepository.setOrbitClockwise(true) },
            )
            OrbitDirectionChoice(
                label = stringResource(R.string.orbit_counterclockwise),
                selected = !settings.orbitClockwise,
                onSelect = { AudioSettingsRepository.setOrbitClockwise(false) },
            )
        }
        // SMCPKG_SUPPORT<<<Cursor121
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.orbit_strength),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.orbit_strength_value, settings.orbitStrength * 100f),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = settings.orbitStrength,
            onValueChange = AudioSettingsRepository::setOrbitStrength,
            valueRange = 0f..1f,
        )
    }
}

@Composable
private fun OrbitDirectionChoice(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(
            text = label,
            modifier = Modifier
                .clickable(onClick = onSelect)
                .padding(end = 12.dp),
        )
    }
}

@Composable
private fun SettingsClickRow(
    title: String,
    summary: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // SMCPKG_SUPPORT>>>Cursor121
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp),
        )
        // SMCPKG_SUPPORT<<<Cursor121
    }
}

@Composable
private fun SettingsValueRow(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = accentColor(),
            textAlign = TextAlign.End,
            modifier = Modifier
                .weight(1.2f)
                .padding(start = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = 4.dp)
                .size(20.dp),
        )
    }
}

private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

private fun Context.openPrivacyPolicy() {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL))
    if (findActivity() == null) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { startActivity(intent) }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // SMCPKG_SUPPORT>>>Cursor121
            // .padding(horizontal = 16.dp, vertical = 10.dp),
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
            // SMCPKG_SUPPORT<<<Cursor121
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            // SMCPKG_SUPPORT>>>Cursor121
            // onCheckedChange = onCheckedChange,
            onCheckedChange = null,
            modifier = Modifier.clearAndSetSemantics {},
            // SMCPKG_SUPPORT<<<Cursor121
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Settings light", showBackground = true, widthDp = 360, heightDp = 2200)
@Composable
private fun SettingsLightPreview() {
    Go2RadioTheme(darkTheme = false) {
        SettingsScaffold(onBack = {}) {
            SettingsBody(
                settings = AudioSettings(
                    showPanelButton = true,
                    voiceEnhancement = true,
                    spatialMode = SpatialAudioMode.ORBIT,
                    bassDb = 3f,
                ),
                removeAds = RemoveAdsUiState(
                    entitled = false,
                    productAvailable = true,
                    formattedPrice = "US$2.99",
                    detailsSettled = true,
                    billingReady = true,
                ),
                showPrivacyOptions = true,
                showDebugOverride = true,
                languageTag = AppLanguage.TRADITIONAL,
                speakerOnly = true,
                onBuy = {},
                onRestore = {},
                onPrivacyOptions = {},
                onPrivacyPolicy = {},
                onLanguageSelected = {},
                onDebugOverride = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Settings dark",
    showBackground = true,
    widthDp = 360,
    heightDp = 1800,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun SettingsDarkPreview() {
    Go2RadioTheme(darkTheme = true) {
        SettingsScaffold(onBack = {}) {
            SettingsBody(
                settings = AudioSettings(spatialMode = SpatialAudioMode.NATURAL),
                removeAds = RemoveAdsUiState(
                    entitled = true,
                    detailsSettled = true,
                    billingReady = true,
                ),
                showPrivacyOptions = false,
                showDebugOverride = false,
                languageTag = AppLanguage.ENGLISH,
                speakerOnly = true,
                onBuy = {},
                onRestore = {},
                onPrivacyOptions = {},
                onPrivacyPolicy = {},
                onLanguageSelected = {},
                onDebugOverride = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Settings small", showBackground = true, widthDp = 320, heightDp = 2400)
@Composable
private fun SettingsSmallPreview() {
    Go2RadioTheme(darkTheme = false) {
        SettingsScaffold(onBack = {}) {
            SettingsBody(
                settings = AudioSettings(
                    spatialMode = SpatialAudioMode.ORBIT,
                    orbitClockwise = false,
                ),
                removeAds = RemoveAdsUiState(
                    entitled = false,
                    pending = true,
                    detailsSettled = true,
                ),
                showPrivacyOptions = true,
                showDebugOverride = false,
                languageTag = AppLanguage.SYSTEM,
                speakerOnly = false,
                onBuy = {},
                onRestore = {},
                onPrivacyOptions = {},
                onPrivacyPolicy = {},
                onLanguageSelected = {},
                onDebugOverride = {},
            )
        }
    }
}
