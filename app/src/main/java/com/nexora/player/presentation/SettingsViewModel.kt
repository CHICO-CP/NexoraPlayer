package com.nexora.player.presentation

class SettingsViewModel {
    fun safeCrossfadeDuration(durationMs: Int): Int = durationMs.coerceIn(500, 5000)
    fun safeSleepMinutes(minutes: Int): Int = minutes.coerceIn(5, 240)
}
