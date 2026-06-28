package com.nexora.player.playback

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CrossfadeController(
    private val scope: CoroutineScope
) {
    private var tickerJob: Job? = null
    private var crossfading = false
    private var enabled = false
    private var durationMs = 3000L

    fun configure(enabled: Boolean, durationMs: Int) {
        this.enabled = enabled
        this.durationMs = durationMs.coerceIn(500, 5000).toLong()
    }

    fun attach(player: ExoPlayer) {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                delay(250L)
                if (!enabled || crossfading || !player.isPlaying) continue
                val duration = player.duration.takeIf { it > 0L } ?: continue
                val remaining = duration - player.currentPosition
                val hasNext = player.hasNextMediaItem()
                if (hasNext && remaining in 1L..durationMs) {
                    crossfadeToNext(player)
                }
            }
        }
    }

    fun detach() {
        tickerJob?.cancel()
        tickerJob = null
        crossfading = false
    }

    fun onManualTransition(player: Player) {
        if (!enabled) return
        val exo = player as? ExoPlayer ?: return
        scope.launch { fade(exo, exo.volume, 1f, durationMs) }
    }

    private fun crossfadeToNext(player: ExoPlayer) {
        scope.launch {
            crossfading = true
            try {
                fade(player, player.volume, 0.08f, durationMs / 2)
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.playWhenReady = true
                    player.play()
                }
                fade(player, 0.08f, 1f, durationMs / 2)
            } finally {
                player.volume = 1f
                crossfading = false
            }
        }
    }

    private suspend fun fade(player: ExoPlayer, from: Float, to: Float, duration: Long) {
        val steps = 18
        val safeDuration = duration.coerceAtLeast(1L)
        repeat(steps) { index ->
            val fraction = (index + 1).toFloat() / steps.toFloat()
            player.volume = (from + (to - from) * fraction).coerceIn(0f, 1f)
            delay(safeDuration / steps)
        }
    }
}
