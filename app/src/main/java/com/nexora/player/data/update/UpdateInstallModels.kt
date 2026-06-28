package com.nexora.player.data.update

data class UpdateInstallState(
    val active: Boolean = false,
    val downloading: Boolean = false,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val message: String? = null,
    val error: String? = null,
    val waitingForInstallPermission: Boolean = false
)
