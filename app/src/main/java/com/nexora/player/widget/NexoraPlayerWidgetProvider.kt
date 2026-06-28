package com.nexora.player.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import com.nexora.player.MainActivity
import com.nexora.player.R
import com.nexora.player.data.model.PlaybackSnapshot
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.playback.PlayerService

class NexoraPlayerWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, PlayerEngine.snapshot.value)
    }

    companion object {
        fun updateWidgets(context: Context, snapshot: PlaybackSnapshot = PlayerEngine.snapshot.value) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NexoraPlayerWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return
            ids.forEach { widgetId ->
                manager.updateAppWidget(widgetId, buildViews(context, snapshot))
            }
        }

        private fun buildViews(context: Context, snapshot: PlaybackSnapshot): RemoteViews {
            val current = snapshot.currentItem
            val views = RemoteViews(context.packageName, R.layout.widget_player)
            views.setTextViewText(R.id.widget_title, current?.title ?: context.getString(R.string.app_name))
            views.setTextViewText(
                R.id.widget_subtitle,
                current?.let { item ->
                    listOf(item.artist, item.album).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Reproduciendo en Nexora" }
                } ?: "Toca para abrir la app"
            )
            views.setTextViewText(R.id.widget_play_pause, if (snapshot.isPlaying) "⏸" else "▶")
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
            return PendingIntent.getService(context, requestCode, intent, flags)
        }

        private fun immutableFlag(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE
        } else 0
    }
}
