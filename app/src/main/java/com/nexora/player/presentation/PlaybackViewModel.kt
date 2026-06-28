package com.nexora.player.presentation

import androidx.media3.common.Player
import com.nexora.player.data.model.NexoraRepeatMode

class PlaybackViewModel {
    fun toPlayerRepeatMode(mode: NexoraRepeatMode): Int = when (mode) {
        NexoraRepeatMode.OFF -> Player.REPEAT_MODE_OFF
        NexoraRepeatMode.ONE -> Player.REPEAT_MODE_ONE
        NexoraRepeatMode.ALL -> Player.REPEAT_MODE_ALL
    }
}
