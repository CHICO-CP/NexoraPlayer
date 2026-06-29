package com.nexora.player.data.share

import android.content.Context
import com.nexora.player.BuildConfig
import com.nexora.player.R

object NexoraShareText {
    fun build(context: Context, downloadUrl: String = BuildConfig.NEXORA_SERVER_URL): String {
        val safeUrl = downloadUrl.ifBlank { BuildConfig.NEXORA_SERVER_URL }
        return context.getString(R.string.share_text_body, safeUrl)
    }
}
