package com.nexora.player.data.model

import android.net.Uri
import androidx.annotation.StringRes
import com.nexora.player.R

enum class MediaKind {
    AUDIO,
    VIDEO
}

enum class MediaSource {
    LOCAL,
    ONLINE
}

enum class SortMode(@StringRes val labelRes: Int) {
    DATE_ADDED_DESC(R.string.sort_date_added_desc),
    DATE_ADDED_ASC(R.string.sort_date_added_asc),
    TITLE_ASC(R.string.sort_title_asc),
    TITLE_DESC(R.string.sort_title_desc),
    DURATION_ASC(R.string.sort_duration_asc),
    DURATION_DESC(R.string.sort_duration_desc),
    ARTIST_ASC(R.string.sort_artist_asc),
    ALBUM_ASC(R.string.sort_album_asc),
    FOLDER_ASC(R.string.sort_path_asc),
    FOLDER_DESC(R.string.sort_path_desc),
    SIZE_ASC(R.string.sort_size_asc),
    SIZE_DESC(R.string.sort_size_desc),
    RESOLUTION_ASC(R.string.sort_resolution_asc),
    RESOLUTION_DESC(R.string.sort_resolution_desc)
}

enum class AppThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_system),
    LIGHT(R.string.theme_light),
    DARK(R.string.theme_dark),
    NEXORA_DARK(R.string.theme_nexora_dark),
    IOS_LIGHT(R.string.theme_ios_light),
    AMOLED_BLACK(R.string.theme_amoled_black),
    FLAMINGO(R.string.theme_flamingo),
    NEON(R.string.theme_neon),
    MATERIAL_YOU(R.string.theme_material_you)
}

enum class NexoraRepeatMode {
    OFF,
    ONE,
    ALL;

    fun next(): NexoraRepeatMode = when (this) {
        OFF -> ONE
        ONE -> ALL
        ALL -> OFF
    }
}

enum class AppLanguage(
    @StringRes val labelRes: Int,
    val tag: String?,
    @StringRes val subtitleRes: Int,
    val badge: String
) {
    SYSTEM(R.string.language_system, null, R.string.language_subtitle_system, "AUTO"),
    SPANISH(R.string.language_spanish, "es", R.string.language_subtitle_spanish, "ES"),
    ENGLISH(R.string.language_english, "en", R.string.language_subtitle_english, "EN"),
    FRENCH(R.string.language_french, "fr", R.string.language_subtitle_french, "FR"),
    PORTUGUESE(R.string.language_portuguese, "pt", R.string.language_subtitle_portuguese, "PT"),
    GERMAN(R.string.language_german, "de", R.string.language_subtitle_german, "DE"),
    ITALIAN(R.string.language_italian, "it", R.string.language_subtitle_italian, "IT"),
    JAPANESE(R.string.language_japanese, "ja", R.string.language_subtitle_japanese, "JA"),
    KOREAN(R.string.language_korean, "ko", R.string.language_subtitle_korean, "KO"),
    CHINESE(R.string.language_chinese, "zh", R.string.language_subtitle_chinese, "ZH"),
    RUSSIAN(R.string.language_russian, "ru", R.string.language_subtitle_russian, "RU"),
    ARABIC(R.string.language_arabic, "ar", R.string.language_subtitle_arabic, "AR"),
    HINDI(R.string.language_hindi, "hi", R.string.language_subtitle_hindi, "HI"),
    INDONESIAN(R.string.language_indonesian, "id", R.string.language_subtitle_indonesian, "ID"),
    TURKISH(R.string.language_turkish, "tr", R.string.language_subtitle_turkish, "TR");

    companion object {
        fun fromTag(tag: String?): AppLanguage {
            val normalized = tag
                ?.split(',')
                ?.firstOrNull()
                ?.trim()
                ?.lowercase()
                .orEmpty()
            return when {
                normalized.isBlank() -> SYSTEM
                normalized.startsWith("es") -> SPANISH
                normalized.startsWith("en") -> ENGLISH
                normalized.startsWith("fr") -> FRENCH
                normalized.startsWith("pt") -> PORTUGUESE
                normalized.startsWith("de") -> GERMAN
                normalized.startsWith("it") -> ITALIAN
                normalized.startsWith("ja") -> JAPANESE
                normalized.startsWith("ko") -> KOREAN
                normalized.startsWith("zh") -> CHINESE
                normalized.startsWith("ru") -> RUSSIAN
                normalized.startsWith("ar") -> ARABIC
                normalized.startsWith("hi") -> HINDI
                normalized.startsWith("id") || normalized.startsWith("in") -> INDONESIAN
                normalized.startsWith("tr") -> TURKISH
                else -> SYSTEM
            }
        }
    }
}

enum class AppDestination(@StringRes val labelRes: Int) {
    ONLINE(R.string.nav_online),
    MUSIC(R.string.nav_music),
    VIDEOS(R.string.nav_videos),
    QUEUE(R.string.nav_queue),
    PLAYLISTS(R.string.nav_playlists),
    FAVORITES(R.string.nav_favorites),
    HISTORY(R.string.nav_history),
    SETTINGS(R.string.nav_settings)
}

data class MediaEntry(
    val id: Long,
    val kind: MediaKind,
    val uri: Uri,
    val title: String,
    val subtitle: String = "",
    val album: String = "",
    val artist: String = "",
    val durationMs: Long = 0L,
    val dateAdded: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int? = null,
    val height: Int? = null,
    val mimeType: String? = null,
    val folder: String? = null,
    val albumId: Long? = null,
    val source: MediaSource = MediaSource.LOCAL,
    val onlineId: String? = null,
    val artworkUrl: String? = null
) {
    val resolutionLabel: String
        get() = if (width != null && height != null) "${width}x$height" else "—"
}

data class PlaybackSnapshot(
    val queue: List<MediaEntry> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false
) {
    val currentItem: MediaEntry?
        get() = queue.getOrNull(currentIndex)
}
