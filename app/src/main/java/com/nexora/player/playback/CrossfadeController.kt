package com.nexora.player.playback

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToLong

/**
 * Crossfade estable para la cola de audio.
 *
 * Media3/ExoPlayer no trae un crossfade real de dos pistas listo para activar.
 * Este controlador evita el fallo anterior donde se saltaba anticipadamente a la
 * siguiente canción y rompía la transición: ahora detecta el tramo final, baja
 * volumen progresivamente, deja que ExoPlayer cambie de item de forma natural y
 * sube el volumen al entrar la nueva canción.
 */
class CrossfadeController(
    private val scope: CoroutineScope
) {
    private var monitorJob: Job? = null
    private var fadeJob: Job? = null
    private var enabled = false
    private var durationMs = 3000L
    private var targetVolume = 1f
    private var fadingOutForIndex = -1

    fun configure(enabled: Boolean, durationMs: Int, targetVolume: Float = this.targetVolume) {
        this.enabled = enabled
        this.durationMs = durationMs.coerceIn(800, 7000).toLong()
        this.targetVolume = targetVolume.coerceIn(0f, 1f)
        if (!enabled) {
            fadeJob?.cancel()
            fadingOutForIndex = -1
        }
    }

    fun setTargetVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        if (!enabled) return
    }

    fun attach(player: ExoPlayer) {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                delay(120L)
                if (!enabled || !player.isPlaying || player.mediaItemCount <= 1) continue
                if (!player.hasNextMediaItem()) continue

                val duration = player.duration.takeIf { it > 0L } ?: continue
                val position = player.currentPosition.coerceAtLeast(0L)
                val remaining = (duration - position).coerceAtLeast(0L)
                val index = player.currentMediaItemIndex

                if (remaining in 1L..durationMs) {
                    fadingOutForIndex = index
                    val progress = 1f - (remaining.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                    val minVolume = (targetVolume * 0.12f).coerceIn(0.02f, targetVolume)
                    val nextVolume = targetVolume - ((targetVolume - minVolume) * progress)
                    player.volume = nextVolume.coerceIn(minVolume, targetVolume)
                } else if (fadingOutForIndex == index && remaining > durationMs + 500L) {
                    fadingOutForIndex = -1
                    player.volume = targetVolume
                }
            }
        }
    }

    fun detach() {
        monitorJob?.cancel()
        fadeJob?.cancel()
        monitorJob = null
        fadeJob = null
        fadingOutForIndex = -1
    }

    fun onManualTransition(player: Player) {
        if (!enabled) return
        val exo = player as? ExoPlayer ?: return
        fadingOutForIndex = -1
        fadeIn(exo)
    }

    private fun fadeIn(player: ExoPlayer) {
        fadeJob?.cancel()
        fadeJob = scope.launch {
            val from = player.volume.coerceIn(0f, targetVolume)
            val steps = 22
            val safeDuration = (durationMs * 0.65f).roundToLong().coerceAtLeast(300L)
            repeat(steps) { index ->
                if (!enabled || !isActive) return@launch
                val fraction = (index + 1).toFloat() / steps.toFloat()
                player.volume = (from + (targetVolume - from) * fraction).coerceIn(0f, targetVolume)
                delay(safeDuration / steps)
            }
            player.volume = targetVolume
        }
    }
}
