package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R
import com.apsmkimo.go2radiolive.data.remote.AppleStorefronts
import com.apsmkimo.go2radiolive.data.remote.storefrontDisplayName
import java.text.Collator
import java.util.Locale

@Composable
fun PodcastRegionDialog(
    selectedCode: String,
    deviceRegionCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val locale = LocalConfiguration.current.locales.let { locales ->
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
    var query by remember { mutableStateOf("") }
    val automaticLabel = stringResource(R.string.podcast_region_automatic)
    val deviceName = remember(deviceRegionCode, locale) {
        val code = deviceRegionCode.trim()
        if (code.length == 2) storefrontDisplayName(code, locale) else ""
    }
    val trimmed = query.trim()
    val showAutomatic = trimmed.isEmpty() ||
        automaticLabel.contains(trimmed, ignoreCase = true) ||
        deviceName.contains(trimmed, ignoreCase = true) ||
        deviceRegionCode.contains(trimmed, ignoreCase = true)
    val countries = remember(locale, trimmed) {
        val collator = Collator.getInstance(locale)
        AppleStorefronts.codes
            .map { code -> code to storefrontDisplayName(code, locale) }
            .filter { (code, name) ->
                trimmed.isEmpty() ||
                    name.contains(trimmed, ignoreCase = true) ||
                    code.contains(trimmed, ignoreCase = true)
            }
            .sortedWith { left, right -> collator.compare(left.second, right.second) }
    }
    val selected = selectedCode.trim().lowercase()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.podcast_region)) },
        text = {
            Column(modifier = Modifier.height(420.dp).fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.podcast_region_search)) },
                )
                LazyColumn(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                    if (showAutomatic) {
                        item(key = "automatic") {
                            RegionRow(
                                title = automaticLabel,
                                subtitle = if (deviceName.isBlank()) {
                                    null
                                } else {
                                    stringResource(R.string.podcast_region_device, deviceName)
                                },
                                selected = selected.isEmpty(),
                                onClick = { onSelect("") },
                            )
                        }
                    }
                    items(countries, key = { it.first }) { (code, name) ->
                        RegionRow(
                            title = name,
                            subtitle = code.uppercase(locale),
                            selected = selected == code,
                            onClick = { onSelect(code) },
                        )
                    }
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
private fun RegionRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = if (selected) {
        ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    } else {
        ListItemDefaults.colors()
    }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle == null) {
            null
        } else {
            { Text(subtitle) }
        },
        trailingContent = if (selected) {
            { Icon(Icons.Outlined.Check, contentDescription = null) }
        } else {
            null
        },
        colors = colors,
        modifier = Modifier.clickable(onClick = onClick),
    )
}
