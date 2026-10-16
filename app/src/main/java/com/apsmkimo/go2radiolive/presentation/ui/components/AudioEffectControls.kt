package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.domain.model.AudioSettings

@Composable
fun AudioEffectControls(
    settings: AudioSettings,
    onBassChange: (Float) -> Unit,
    onLowChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onHighChange: (Float) -> Unit,
    onBalanceChange: (Float) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        LevelSlider(
            label = stringResource(R.string.eq_bass),
            valueLabel = stringResource(R.string.db_value, settings.bassDb),
            value = settings.bassDb,
            valueRange = AudioSettings.BAND_DB_MIN..AudioSettings.BAND_DB_MAX,
            onValueChange = onBassChange,
        )
        LevelSlider(
            label = stringResource(R.string.eq_low),
            valueLabel = stringResource(R.string.db_value, settings.lowDb),
            value = settings.lowDb,
            valueRange = AudioSettings.BAND_DB_MIN..AudioSettings.BAND_DB_MAX,
            onValueChange = onLowChange,
        )
        LevelSlider(
            label = stringResource(R.string.eq_mid),
            valueLabel = stringResource(R.string.db_value, settings.midDb),
            value = settings.midDb,
            valueRange = AudioSettings.BAND_DB_MIN..AudioSettings.BAND_DB_MAX,
            onValueChange = onMidChange,
        )
        LevelSlider(
            label = stringResource(R.string.eq_high),
            valueLabel = stringResource(R.string.db_value, settings.highDb),
            value = settings.highDb,
            valueRange = AudioSettings.BAND_DB_MIN..AudioSettings.BAND_DB_MAX,
            onValueChange = onHighChange,
        )
        LevelSlider(
            label = stringResource(R.string.balance),
            valueLabel = stringResource(R.string.balance_value, settings.balance),
            value = settings.balance,
            valueRange = AudioSettings.BALANCE_MIN..AudioSettings.BALANCE_MAX,
            onValueChange = onBalanceChange,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.balance_left),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.balance_right),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.reset))
            }
        }
    }
}

@Composable
private fun LevelSlider(
    label: String,
    valueLabel: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}
