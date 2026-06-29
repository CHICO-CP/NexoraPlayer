package com.nexora.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.nexora.player.MainActivity
import com.nexora.player.R
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.lyrics.LrcParser
import com.nexora.player.data.lyrics.LyricsSource
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.data.model.PlaybackSnapshot
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.playback.PlayerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NexoraPlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, PlayerEngine.snapshot.value)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgets(context, PlayerEngine.snapshot.value)
    }

    companion object {
        private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateWidgets(context: Context, snapshot: PlaybackSnapshot = PlayerEngine.snapshot.value) {
            val appContext = context.applicationContext
            widgetScope.launch {
                val manager = AppWidgetManager.getInstance(appContext)
                val component = ComponentName(appContext, NexoraPlayerWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids.isEmpty()) return@launch

                val current = snapshot.currentItem
                val database = NexoraDatabase.get(appContext)
                val isFavorite = current?.takeIf { it.kind == MediaKind.AUDIO }?.let { item ->
                    runCatching { database.favoritesDao().isFavorite(item.id, item.kind.name) }.getOrDefault(false)
                } ?: false
                val positionMs = PlayerEngine.currentPositionMs()
                val lyricLine = current?.takeIf { it.kind == MediaKind.AUDIO }?.let { item ->
                    runCatching {
                        database.lyricsDao().getByMediaId(item.id)?.rawText?.toWidgetLyricLine(
                            positionMs = positionMs,
                            item = item
                        )
                    }.getOrNull()
                }.orEmpty()
                val artwork = current?.let { loadArtworkBitmap(appContext, it) }

                withContext(Dispatchers.Main) {
                    ids.forEach { widgetId ->
                        val options = manager.getAppWidgetOptions(widgetId)
                        manager.updateAppWidget(
                            widgetId,
                            buildViews(
                                context = appContext,
                                snapshot = snapshot,
                                isFavorite = isFavorite,
                                lyricLine = lyricLine,
                                artwork = artwork,
                                options = options
                            )
                        )
                    }
                }
            }
        }

        private fun buildViews(
            context: Context,
            snapshot: PlaybackSnapshot,
            isFavorite: Boolean,
            lyricLine: String,
            artwork: Bitmap?,
            options: Bundle
        ): RemoteViews {
            val current = snapshot.currentItem
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
            val compact = minHeight < 105 || minWidth < 170
            val expanded = minHeight >= 145 && minWidth >= 220

            views.setTextViewText(R.id.widget_title, current?.title ?: context.getString(R.string.app_name))
            views.setTextViewText(
                R.id.widget_subtitle,
                current?.let { item ->
                    listOf(item.artist, item.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" • ")
                        .ifBlank { context.getString(R.string.widget_playing_nexora) }
                } ?: context.getString(R.string.widget_tap_open)
            )
            views.setTextViewText(
                R.id.widget_lyric,
                lyricLine.ifBlank { if (current?.kind == MediaKind.AUDIO) context.getString(R.string.widget_no_lyrics) else context.getString(R.string.widget_playback) }
            )

            if (artwork != null) {
                views.setImageViewBitmap(R.id.widget_artwork, artwork.scaledForWidget())
            } else {
                views.setImageViewResource(R.id.widget_artwork, R.drawable.ic_media_placeholder)
            }

            views.setImageViewResource(
                R.id.widget_play_pause,
                if (snapshot.isPlaying) R.drawable.ic_notification_pause else R.drawable.ic_notification_playback
            )
            views.setImageViewResource(
                R.id.widget_favorite,
                if (isFavorite) R.drawable.ic_notification_favorite_filled else R.drawable.ic_notification_favorite_outline
            )
            views.setViewVisibility(R.id.widget_artwork, if (compact) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_lyric, if (expanded) View.VISIBLE else View.GONE)

            views.setOnClickPendingIntent(R.id.widget_root, activityIntent(context))
            views.setOnClickPendingIntent(R.id.widget_previous, serviceIntent(context, PlayerService.ACTION_PREVIOUS, 1))
            views.setOnClickPendingIntent(R.id.widget_play_pause, serviceIntent(context, PlayerService.ACTION_PLAY_PAUSE, 2))
            views.setOnClickPendingIntent(R.id.widget_next, serviceIntent(context, PlayerService.ACTION_NEXT, 3))
            views.setOnClickPendingIntent(R.id.widget_favorite, serviceIntent(context, PlayerService.ACTION_FAVORITE, 4))
            return views
        }

        private fun activityIntent(context: Context): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            return PendingIntent.getActivity(
                context,
                100,
                Intent(context, MainActivity::class.java),
                flags
            )
        }

        private fun serviceIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or immutableFlag()
            val intent = Intent(context, PlayerService::class.java).setAction(action)
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(context, requestCode, intent, flags)
            } else {
                PendingIntent.getService(context, requestCode, intent, flags)
            }
        }

        private fun Bitmap.scaledForWidget(maxSide: Int = 256): Bitmap {
            if (width <= maxSide && height <= maxSide) return this
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth: Int
            val targetHeight: Int
            if (width >= height) {
                targetWidth = maxSide
                targetHeight = (maxSide / ratio).toInt().coerceAtLeast(1)
            } else {
                targetHeight = maxSide
                targetWidth = (maxSide * ratio).toInt().coerceAtLeast(1)
            }
            return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
        }

        private fun immutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0

        private fun String.toWidgetLyricLine(positionMs: Long, item: MediaEntry): String {
            val parsed = runCatching {
                LrcParser.parse(
                    rawText = this,
                    mediaId = item.id,
                    title = item.title,
                    artist = item.artist,
                    album = item.album,
                    source = LyricsSource.DATABASE
                )
            }.getOrNull()

            val syncedLine = parsed
                ?.takeIf { it.isSynced && it.lines.isNotEmpty() }
                ?.let { lyrics -> lyrics.lines.getOrNull(lyrics.currentLineIndex(positionMs))?.text }
                ?.cleanWidgetLyric()
                ?.takeIf { it.isNotBlank() }

            if (!syncedLine.isNullOrBlank()) return syncedLine.take(110)

            return lineSequence()
                .map { it.cleanWidgetLyric() }
                .firstOrNull { it.isNotBlank() && !it.startsWith("#") }
                ?.take(110)
                .orEmpty()
        }

        private fun String.cleanWidgetLyric(): String {
            return replace(Regex("\\[[^]]+\\]"), "")
                .replace(Regex("<[^>]+>"), "")
                .trim()
        }

        private fun loadArtworkBitmap(context: Context, item: MediaEntry): Bitmap? {
            val retriever = MediaMetadataRetriever()
            return runCatching {
                retriever.setDataSource(context, item.uri)
                val bytes = retriever.embeddedPicture ?: return@runCatching null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }.getOrNull().also {
                runCatching { retriever.release() }
            }
        }
    }
}
