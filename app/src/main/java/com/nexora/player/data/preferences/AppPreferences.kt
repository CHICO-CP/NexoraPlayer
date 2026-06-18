package com.nexora.player.data.preferences

import com.nexora.player.data.model.AppDestination
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.data.model.DownloadStorageMode
import com.nexora.player.data.model.SortMode

data class AppPreferences(
    val audioSort: SortMode = SortMode.DATE_ADDED_DESC,
    val videoSort: SortMode = SortMode.DATE_ADDED_DESC,
    val lastDestination: AppDestination = AppDestination.MUSIC,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val onlineMusicSearchEnabled: Boolean = true,
    val downloadStorageMode: DownloadStorageMode = DownloadStorageMode.ASK_FIRST_TIME,
    val hiddenAudioIds: Set<Long> = emptySet()
)
