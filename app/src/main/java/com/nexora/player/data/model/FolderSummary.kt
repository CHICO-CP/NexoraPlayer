package com.nexora.player.data.model

data class FolderSummary(
    val path: String,
    val name: String,
    val songCount: Int,
    val totalSizeBytes: Long
)
