package com.nexora.player.playback

import androidx.media3.exoplayer.ExoPlayer
import com.nexora.player.data.preferences.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object SleepTimerController {
    private var timerJob: Job? = null
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    fun schedule(
        scope: CoroutineScope,
        prefs: AppPreferences,
        playerProvider: () -> ExoPlayer,
        onStop: () -> Unit
    ) {
        timerJob?.cancel()
        _remainingMs.value = 0L
        if (!prefs.sleepTimerEnabled) return

        timerJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val player = runCatching { playerProvider() }.getOrNull()
                val remaining = when {
                    prefs.sleepTimerStopAtEndOfTrack && player != null -> {
                        val duration = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        (duration - player.currentPosition).coerceAtLeast(0L)
                    }
                    prefs.sleepTimerEndAtMs > 0L -> (prefs.sleepTimerEndAtMs - now).coerceAtLeast(0L)
                    else -> 0L
                }
                _remainingMs.value = if (remaining == Long.MAX_VALUE) 0L else remaining
                if (remaining <= 0L || remaining <= 1250L && prefs.sleepTimerStopAtEndOfTrack) {
                    player?.let { fadeOut(it) }
                    onStop()
                    _remainingMs.value = 0L
                    break
                }
                delay(1000L)
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingMs.value = 0L
    }

    private suspend fun fadeOut(player: ExoPlayer) {
        val start = player.volume
        repeat(16) { index ->
            val fraction = (index + 1).toFloat() / 16f
            player.volume = (start * (1f - fraction)).coerceIn(0f, 1f)
            delay(55L)
        }
    }
}
