package com.nexora.player.models

import android.net.Uri

data class MediaItem(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long,
    val uri: Uri,
    val albumArtUri: Uri?,
    val mediaType: MediaType,
    val size: Long,
    val dateAdded: Long
)

enum class MediaType { AUDIO, VIDEO }
