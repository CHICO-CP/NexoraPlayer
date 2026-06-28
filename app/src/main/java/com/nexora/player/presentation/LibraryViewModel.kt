package com.nexora.player.presentation

import com.nexora.player.data.model.FolderSummary
import com.nexora.player.data.model.MediaEntry

class LibraryViewModel {
    fun visibleAudio(
        audio: List<MediaEntry>,
        hiddenIds: Set<Long>,
        hiddenFolders: Set<String>
    ): List<MediaEntry> = audio
        .filterNot { it.id in hiddenIds }
        .filterNot { entry ->
            val folder = entry.folder.orEmpty()
            hiddenFolders.any { hidden -> folder.startsWith(hidden) || folder.contains(hidden, ignoreCase = true) }
        }

    fun folderSummaries(audio: List<MediaEntry>): List<FolderSummary> = audio
        .filter { it.kind.name == "AUDIO" }
        .groupBy { it.folder.orEmpty().ifBlank { "Sin carpeta" } }
        .map { (path, entries) ->
            FolderSummary(
                path = path,
                name = path.substringAfterLast('/').ifBlank { path },
                songCount = entries.size,
                totalSizeBytes = entries.sumOf { it.sizeBytes }
            )
        }
        .sortedWith(compareByDescending<FolderSummary> { it.songCount }.thenBy { it.path.lowercase() })

    fun suggestedNoiseFolders(folders: List<FolderSummary>): Set<String> {
        val names = listOf("Notifications", "Ringtones", "Alarms", "WhatsApp Audio", "Recordings")
        return folders.mapNotNull { folder ->
            names.firstOrNull { folder.path.contains(it, ignoreCase = true) }?.let { folder.path }
        }.toSet()
    }
}
