package com.nexora.player.utils

object TimeUtils {
    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return "0:00"
        val totalSec = durationMs / 1000
        val hours   = totalSec / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60
        return if (hours > 0)
            "%d:%02d:%02d".format(hours, minutes, seconds)
        else
            "%d:%02d".format(minutes, seconds)
    }
}
