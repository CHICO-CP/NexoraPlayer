package com.nexora.player.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind

object MediaLibraryNotifier {
    private const val CHANNEL_ID = "nexora_library_changes"
    private const val CHANNEL_NAME = "Nuevos archivos"
    private const val PREFS_NAME = "nexora_media_monitor"
    private const val KEY_AUDIO_MARKER = "audio_marker"
    private const val KEY_VIDEO_MARKER = "video_marker"
    private const val KEY_INITIALIZED = "initialized"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos cuando se detectan nuevos audios o videos"
        }
        manager.createNotificationChannel(channel)
    }

    fun maybeNotify(context: Context, audio: List<MediaEntry>, video: List<MediaEntry>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val initialized = prefs.getBoolean(KEY_INITIALIZED, false)
        val audioMarker = audio.maxOfOrNull { it.dateAdded } ?: 0L
        val videoMarker = video.maxOfOrNull { it.dateAdded } ?: 0L
        val lastAudio = prefs.getLong(KEY_AUDIO_MARKER, 0L)
        val lastVideo = prefs.getLong(KEY_VIDEO_MARKER, 0L)

        if (!initialized) {
            prefs.edit()
                .putBoolean(KEY_INITIALIZED, true)
                .putLong(KEY_AUDIO_MARKER, audioMarker)
                .putLong(KEY_VIDEO_MARKER, videoMarker)
                .apply()
            return
        }

        val newAudio = audio.count { it.dateAdded > lastAudio }
        val newVideo = video.count { it.dateAdded > lastVideo }
        if (newAudio <= 0 && newVideo <= 0) {
            prefs.edit()
                .putLong(KEY_AUDIO_MARKER, maxOf(lastAudio, audioMarker))
                .putLong(KEY_VIDEO_MARKER, maxOf(lastVideo, videoMarker))
                .apply()
            return
        }

        val title = when {
            newAudio > 0 && newVideo > 0 -> context.getString(R.string.app_name)
            newAudio > 0 -> "Nueva música detectada"
            else -> "Nuevo video detectado"
        }
        val text = when {
            newAudio > 0 && newVideo > 0 -> "$newAudio audios y $newVideo videos ya están listos en la app."
            newAudio > 0 -> "$newAudio audios ya están listos en la app."
            else -> "$newVideo videos ya están listos en la app."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(2205, notification)
        }

        prefs.edit()
            .putLong(KEY_AUDIO_MARKER, maxOf(lastAudio, audioMarker))
            .putLong(KEY_VIDEO_MARKER, maxOf(lastVideo, videoMarker))
            .apply()
    }
}
