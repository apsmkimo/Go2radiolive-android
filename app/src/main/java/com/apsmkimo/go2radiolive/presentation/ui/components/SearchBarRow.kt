package com.apsmkimo.go2radiolive.presentation.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    onPickPodcastRegion: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
        )
        IconButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = stringResource(R.string.refresh))
        }
        // SMCPKG_SUPPORT>>>Cursor092
        if (onPickPodcastRegion != null) {
            IconButton(onClick = onPickPodcastRegion) {
                Icon(Icons.Outlined.Public, contentDescription = stringResource(R.string.podcast_region))
            }
        }
        // SMCPKG_SUPPORT<<<Cursor092
        IconButton(
            onClick = {
                Toast.makeText(context, context.getString(R.string.alarm_coming_soon), Toast.LENGTH_SHORT).show()
            },
        ) {
            Icon(Icons.Outlined.AccessAlarm, contentDescription = stringResource(R.string.alarm))
        }
    }
}
