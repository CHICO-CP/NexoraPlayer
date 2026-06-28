package com.nexora.player.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry

@Composable
fun MediaItemRow(
    item: MediaEntry,
    modifier: Modifier = Modifier,
    isFavorite: Boolean = false,
    onClick: () -> Unit,
    onFavoriteClick: (() -> Unit)? = null,
    onMoreClick: (() -> Unit)? = null
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MediaArtwork(
                item = item,
                modifier = Modifier.size(52.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val subtitle = buildString {
                    if (item.artist.isNotBlank()) append(item.artist)
                    if (item.album.isNotBlank()) {
                        if (isNotEmpty()) append(" • ")
                        append(item.album)
                    }
                    if (isEmpty()) append(item.folder.orEmpty().ifBlank { formatDuration(item.durationMs) })
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${formatDuration(item.durationMs)} · ${formatBytes(item.sizeBytes)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onFavoriteClick != null) {
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.media_favorite_remove) else stringResource(R.string.media_favorite_add)
                    )
                }
            }

            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.media_more_actions))
                }
            }
        }
        HorizontalDivider(thickness = 0.4.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}
