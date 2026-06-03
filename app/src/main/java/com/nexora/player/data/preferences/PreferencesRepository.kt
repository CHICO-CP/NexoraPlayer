package com.nexora.player.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nexora.player.data.model.AppDestination
import com.nexora.player.data.model.AppThemeMode
import com.nexora.player.data.model.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nexora_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val AUDIO_SORT = stringPreferencesKey("audio_sort")
        val VIDEO_SORT = stringPreferencesKey("video_sort")
        val LAST_DESTINATION = stringPreferencesKey("last_destination")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
    }

    val preferences: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            audioSort = prefs.stringValue(Keys.AUDIO_SORT, SortMode.DATE_ADDED_DESC.name).toSortMode(),
            videoSort = prefs.stringValue(Keys.VIDEO_SORT, SortMode.DATE_ADDED_DESC.name).toSortMode(),
            lastDestination = prefs.stringValue(Keys.LAST_DESTINATION, AppDestination.MUSIC.name).toDestination(),
            themeMode = prefs.stringValue(Keys.THEME_MODE, AppThemeMode.SYSTEM.name).toThemeMode(),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true
        )
    }

    suspend fun setAudioSort(sortMode: SortMode) {
        context.dataStore.edit { it[Keys.AUDIO_SORT] = sortMode.name }
    }

    suspend fun setVideoSort(sortMode: SortMode) {
        context.dataStore.edit { it[Keys.VIDEO_SORT] = sortMode.name }
    }

    suspend fun setLastDestination(destination: AppDestination) {
        context.dataStore.edit { it[Keys.LAST_DESTINATION] = destination.name }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }
}

private fun Preferences.stringValue(key: Preferences.Key<String>, default: String): String =
    this[key] ?: default

private fun String.toSortMode(): SortMode = runCatching { SortMode.valueOf(this) }.getOrDefault(SortMode.DATE_ADDED_DESC)
private fun String.toDestination(): AppDestination = runCatching { AppDestination.valueOf(this) }.getOrDefault(AppDestination.MUSIC)
private fun String.toThemeMode(): AppThemeMode = runCatching { AppThemeMode.valueOf(this) }.getOrDefault(AppThemeMode.SYSTEM)
