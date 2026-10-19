package com.apsmkimo.go2radiolive.presentation.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apsmkimo.go2radiolive.BuildConfig
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.ads.AdsConsent
import com.apsmkimo.go2radiolive.audio.AudioSettingsRepository
import com.apsmkimo.go2radiolive.domain.model.AudioSettings
import com.apsmkimo.go2radiolive.domain.model.SpatialAudioMode
import com.apsmkimo.go2radiolive.presentation.ui.components.AudioEffectControls

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val settings by AudioSettingsRepository.settings.collectAsStateWithLifecycle()
    val privacyOptionsRequired by AdsConsent.privacyOptionsRequired.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

    Scaffold(
        topBar = {
            TopAppBar(
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
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.settings_version_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.settings_version_value,
                            BuildConfig.VERSION_NAME,
                            BuildConfig.VERSION_CODE,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (privacyOptionsRequired) {
                SettingsClickRow(
                    title = stringResource(R.string.privacy_options),
                    summary = stringResource(R.string.privacy_options_summary),
                    onClick = { activity?.let(AdsConsent::showPrivacyOptions) },
                )
            }

            SettingsSwitchRow(
                title = stringResource(R.string.audio_panel_button),
                summary = stringResource(R.string.audio_panel_button_summary),
                checked = settings.showPanelButton,
                onCheckedChange = AudioSettingsRepository::setShowPanelButton,
            )
            SettingsSwitchRow(
                title = stringResource(R.string.voice_enhancement),
                summary = stringResource(R.string.voice_enhancement_summary),
                checked = settings.voiceEnhancement,
                onCheckedChange = AudioSettingsRepository::setVoiceEnhancement,
            )
            // SMCPKG_SUPPORT>>>Cursor064
            // SettingsSwitchRow(
            //     title = stringResource(R.string.virtual_surround),
            //     summary = stringResource(R.string.virtual_surround_summary),
            //     checked = settings.virtualSurround,
            //     onCheckedChange = AudioSettingsRepository::setVirtualSurround,
            // )
            SpatialAudioSection(settings = settings)
            // SMCPKG_SUPPORT<<<Cursor064

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            Text(
                text = stringResource(R.string.eq_section),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
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
    }
}

@Composable
private fun SpatialAudioSection(settings: AudioSettings) {
    Text(
        text = stringResource(R.string.spatial_section),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
    )
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
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

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
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = settings.orbitClockwise,
                onClick = { AudioSettingsRepository.setOrbitClockwise(true) },
            )
            Text(
                text = stringResource(R.string.orbit_clockwise),
                modifier = Modifier
                    .clickable { AudioSettingsRepository.setOrbitClockwise(true) }
                    .padding(end = 12.dp),
            )
            RadioButton(
                selected = !settings.orbitClockwise,
                onClick = { AudioSettingsRepository.setOrbitClockwise(false) },
            )
            Text(
                text = stringResource(R.string.orbit_counterclockwise),
                modifier = Modifier.clickable { AudioSettingsRepository.setOrbitClockwise(false) },
            )
        }
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
