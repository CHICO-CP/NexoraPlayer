package com.nexora.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nexora.player.data.model.MediaEntry

@Composable
fun BottomPlayerBar(
    current: MediaEntry?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    if (current == null) return

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(current.title, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                val subtitle = when {
                    current.artist.isNotBlank() -> current.artist
                    current.album.isNotBlank() -> current.album
                    else -> current.folder.orEmpty()
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            IconButton(onClick = onPrevious) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Anterior")
            }
            IconButton(onClick = onTogglePlay) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.FastForward, contentDescription = "Siguiente")
            }
        }
    }
}
