package com.nexora.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry

@Composable
fun MediaItemRow(
    item: MediaEntry,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null
) {
    Surface(
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                val subtitle = buildString {
                    if (item.artist.isNotBlank()) append(item.artist)
                    if (item.album.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(item.album)
                    }
                    val folder = item.folder.orEmpty()
                    if (folder.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(folder)
                    }
                }
                if (subtitle.isNotBlank()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
                Text(
                    "${formatDuration(item.durationMs)} · ${formatBytes(item.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (onFavoriteClick != null) {
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
