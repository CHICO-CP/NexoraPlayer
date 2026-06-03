package com.nexora.player.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import com.nexora.player.playback.PlayerEngine

@Composable
fun NowPlayingScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val player = PlayerEngine.get(context)
    AndroidView(
        factory = {
            PlayerView(it).apply {
                this.player = player
                useController = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
