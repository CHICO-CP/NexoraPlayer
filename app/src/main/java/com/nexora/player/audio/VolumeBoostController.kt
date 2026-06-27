package com.nexora.player.audio

import android.media.audiofx.LoudnessEnhancer

class VolumeBoostController(audioSessionId: Int) : AutoCloseable {

    private var enhancer: LoudnessEnhancer? = runCatching { LoudnessEnhancer(audioSessionId) }.getOrNull()

    fun setEnabled(enabled: Boolean) {
        runCatching { enhancer?.enabled = enabled }
    }

    fun setGainMillibels(gainMb: Int) {
        runCatching { enhancer?.setTargetGain(gainMb.coerceIn(0, 1800)) }
    }

    override fun close() {
        runCatching { enhancer?.release() }
        enhancer = null
    }
}
