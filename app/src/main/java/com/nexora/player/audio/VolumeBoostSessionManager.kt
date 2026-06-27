package com.nexora.player.audio

object VolumeBoostSessionManager {
    private val lock = Any()
    private var controller: VolumeBoostController? = null
    private var attachedSessionId: Int = Int.MIN_VALUE
    private var enabled: Boolean = false
    private var gainMb: Int = 600

    fun update(enabled: Boolean, gainMillibels: Int) {
        synchronized(lock) {
            this.enabled = enabled
            this.gainMb = gainMillibels.coerceIn(0, 1800)
            controller?.setEnabled(enabled)
            controller?.setGainMillibels(this.gainMb)
        }
    }

    fun attach(audioSessionId: Int) {
        synchronized(lock) {
            if (audioSessionId <= 0) return@synchronized
            if (attachedSessionId == audioSessionId && controller != null) {
                controller?.setEnabled(enabled)
                controller?.setGainMillibels(gainMb)
                return@synchronized
            }
            controller?.close()
            controller = runCatching { VolumeBoostController(audioSessionId) }.getOrNull()
            attachedSessionId = audioSessionId
            controller?.setEnabled(enabled)
            controller?.setGainMillibels(gainMb)
        }
    }

    fun release() {
        synchronized(lock) {
            controller?.close()
            controller = null
            attachedSessionId = Int.MIN_VALUE
        }
    }
}
