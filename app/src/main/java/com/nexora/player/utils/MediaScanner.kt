package com.nexora.player.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.nexora.player.models.MediaItem
import com.nexora.player.models.MediaType

class MediaScanner(private val context: Context) {

    fun scanAudio(): List<MediaItem> {
        val items      = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM_ID
        )
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol      = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol  = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol   = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durCol     = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val sizeCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val dateCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id.toString())
                val albumArtUri = Uri.parse("content://media/external/audio/albumart/${cursor.getLong(albumIdCol)}")
                items += MediaItem(
                    id          = id,
                    title       = cursor.getString(titleCol) ?: "Unknown",
                    artist      = cursor.getString(artistCol) ?: "Unknown Artist",
                    album       = cursor.getString(albumCol) ?: "Unknown Album",
                    duration    = cursor.getLong(durCol),
                    uri         = uri,
                    albumArtUri = albumArtUri,
                    mediaType   = MediaType.AUDIO,
                    size        = cursor.getLong(sizeCol),
                    dateAdded   = cursor.getLong(dateCol)
                )
            }
        }
        return items
    }

    fun scanVideo(): List<MediaItem> {
        val items      = mutableListOf<MediaItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        else MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED
        )
        context.contentResolver.query(
            collection, projection, null, null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol    = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val dateCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id  = cursor.getLong(idCol)
                val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                items += MediaItem(
                    id          = id,
                    title       = cursor.getString(titleCol) ?: "Unknown Video",
                    artist      = "",
                    album       = "",
                    duration    = cursor.getLong(durCol),
                    uri         = uri,
                    albumArtUri = null,
                    mediaType   = MediaType.VIDEO,
                    size        = cursor.getLong(sizeCol),
                    dateAdded   = cursor.getLong(dateCol)
                )
            }
        }
        return items
    }
}
