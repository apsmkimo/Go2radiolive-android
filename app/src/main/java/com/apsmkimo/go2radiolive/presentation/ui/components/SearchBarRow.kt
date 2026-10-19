package com.apsmkimo.go2radiolive.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessAlarm
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apsmkimo.go2radiolive.R

@Composable
fun SearchBarRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isRefreshing: Boolean = false,
    onPickPodcastRegion: (() -> Unit)? = null,
    onAlarm: () -> Unit = {},
    sleepBadge: String? = null,
) {
    // SMCPKG_SUPPORT>>>Cursor096
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            leadingIcon = {
                Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp))
            },
            placeholder = {
                Text(
                    text = stringResource(R.string.search_hint),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
        )
        CompactIconButton(
            onClick = onRefresh,
            enabled = !isRefreshing,
            description = stringResource(R.string.refresh),
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        if (onPickPodcastRegion != null) {
            CompactIconButton(
                onClick = onPickPodcastRegion,
                enabled = true,
                description = stringResource(R.string.podcast_region),
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
        CompactIconButton(
            onClick = onAlarm,
            enabled = true,
            description = stringResource(R.string.alarm),
        ) {
            if (sleepBadge != null) {
                BadgedBox(badge = { Badge { Text(sleepBadge) } }) {
                    Icon(Icons.Outlined.AccessAlarm, contentDescription = null, modifier = Modifier.size(22.dp))
                }
            } else {
                Icon(Icons.Outlined.AccessAlarm, contentDescription = null, modifier = Modifier.size(22.dp))
            }
        }
    }
    // SMCPKG_SUPPORT<<<Cursor096
}

@Composable
private fun CompactIconButton(
    onClick: () -> Unit,
    enabled: Boolean,
    description: String,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(40.dp)
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
