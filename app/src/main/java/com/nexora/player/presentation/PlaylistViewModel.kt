package com.nexora.player.presentation

import com.nexora.player.data.local.PlaybackStatsEntity
import com.nexora.player.data.local.PlaylistItemEntity
import com.nexora.player.data.model.MediaKind
import kotlin.math.absoluteValue

class PlaylistViewModel {
    fun mostPlayedItems(stats: List<PlaybackStatsEntity>, autoPlaylistId: Long): List<PlaylistItemEntity> = stats
        .distinctBy { it.mediaKey }
        .sortedWith(compareByDescending<PlaybackStatsEntity> { it.playCount }.thenByDescending { it.lastPlayedAt })
        .mapIndexed { index, stat ->
            PlaylistItemEntity(
                id = -stat.mediaKey.hashCode().toLong().absoluteValue.coerceAtLeast(1L),
                playlistId = autoPlaylistId,
                mediaId = stat.mediaId,
                mediaKind = stat.mediaKind.ifBlank { MediaKind.AUDIO.name },
                title = stat.title,
                artist = stat.artist,
                album = stat.album,
                durationMs = stat.durationMs,
                uriString = stat.uriString,
                orderIndex = index
            )
        }
}
