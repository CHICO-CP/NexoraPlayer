package com.nexora.player.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.GestureControlOverlay
import com.nexora.player.ui.components.MediaArtwork
import com.nexora.player.ui.components.PlaybackSeekBar
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun VideoPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context.findActivity()
    val exoPlayer = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val audioManager = context.getSystemService<AudioManager>()
    val maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 1
    val currentVolume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
    val volumeFraction = currentVolume.toFloat() / maxVolume.toFloat().coerceAtLeast(1f)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val currentItem = snapshot.currentItem ?: current

    var brightness by remember {
        mutableFloatStateOf(
            activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.65f
        )
    }

    // En portrait los controles son SIEMPRE visibles (true fijo).
    // En landscape se muestran/ocultan con tap (inician visibles).
    var landscapeControlsVisible by remember { mutableStateOf(true) }

    var showQueuePanel by remember { mutableStateOf(false) }
    var seekFeedbackMs by remember { mutableLongStateOf(0L) }

    // ── Sistema de barras / orientación ────────────────────────────────────────
    DisposableEffect(isLandscape) {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.hide(WindowInsetsCompat.Type.systemBars())
            controller?.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            activity?.window
                ?.let { WindowCompat.getInsetsController(it, view) }
                ?.show(WindowInsetsCompat.Type.systemBars())
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // ── Auto-ocultar controles SOLO en landscape ───────────────────────────────
    LaunchedEffect(landscapeControlsVisible, isLandscape) {
        if (isLandscape && landscapeControlsVisible) {
            delay(5_000L)
            landscapeControlsVisible = false
        }
        // En portrait no se toca nada → controles siempre visibles
    }

    // ── Feedback de seek ───────────────────────────────────────────────────────
    LaunchedEffect(seekFeedbackMs) {
        if (seekFeedbackMs != 0L) {
            delay(550)
            seekFeedbackMs = 0L
        }
    }

    BackHandler {
        if (isLandscape) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            onClose()
        }
    }

    fun setBrightness(value: Float) {
        brightness = value.coerceIn(0.05f, 1f)
        activity?.window?.attributes =
            activity?.window?.attributes?.apply { screenBrightness = brightness }
    }

    fun setVolume(value: Float) {
        audioManager?.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            (value.coerceIn(0f, 1f) * maxVolume).roundToInt(),
            0
        )
    }

    fun seekBy(deltaMs: Long) {
        val duration =
            exoPlayer.duration.takeIf { it > 0L } ?: currentItem?.durationMs ?: 0L
        if (duration <= 0L) return
        val target = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, duration)
        exoPlayer.seekTo(target)
        seekFeedbackMs = deltaMs
    }

    if (currentItem == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                stringResource(R.string.video_no_playback),
                style = MaterialTheme.typography.headlineSmall
            )
        }
        return
    }

    val durationMs = exoPlayer.duration.takeIf { it > 0L } ?: currentItem.durationMs
    val queue = snapshot.queue
    val queueFromCurrent = remember(snapshot.queue, snapshot.currentIndex) {
        if (snapshot.currentIndex in queue.indices) queue.drop(snapshot.currentIndex + 1)
        else queue.drop(1)
    }

    // ── Raíz ──────────────────────────────────────────────────────────────────
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        // ── Superficie de video ────────────────────────────────────────────────
        if (isLandscape) {
            LandscapePlayerSurface(
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            PortraitPlayerSurface(
                exoPlayer = exoPlayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .padding(16.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
        }

        // ── Capa de tap ────────────────────────────────────────────────────────
        // En portrait: doble-tap para seek; tap simple no hace nada (controles fijos).
        // En landscape: tap simple muestra/oculta controles; doble-tap hace seek.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f)
                .pointerInput(isLandscape) {
                    detectTapGestures(
                        onTap = { _ ->
                            if (isLandscape) {
                                landscapeControlsVisible = !landscapeControlsVisible
                            }
                            // En portrait no se ocultan, así que no hacemos nada
                        },
                        onDoubleTap = { offset ->
                            if (offset.x < size.width / 2f) seekBy(-10_000L)
                            else seekBy(10_000L)
                            if (isLandscape) landscapeControlsVisible = true
                        }
                    )
                }
        )

        // ── Control por gestos (brillo/volumen) ───────────────────────────────
        GestureControlOverlay(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(0f),
            brightness = brightness,
            volume = volumeFraction,
            onBrightnessChange = ::setBrightness,
            onVolumeChange = ::setVolume
        )

        // ── HUD de seek ────────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = seekFeedbackMs != 0L,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SeekFeedbackHud(
                deltaMs = seekFeedbackMs,
                modifier = Modifier
                    .align(Alignment.Center)
                    .zIndex(2f)
                    .padding(24.dp)
            )
        }

        // ── Overlay de controles ───────────────────────────────────────────────
        // Portrait → siempre visible (no AnimatedVisibility, simplemente siempre compuesto).
        // Landscape → AnimatedVisibility controlado por landscapeControlsVisible.
        if (isLandscape) {
            AnimatedVisibility(
                visible = landscapeControlsVisible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VideoOverlayChrome(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(2f),
                    currentItem = currentItem,
                    queueCount = queue.size,
                    queueFromCurrent = queueFromCurrent,
                    queueStartIndex = if (snapshot.currentIndex in queue.indices)
                        snapshot.currentIndex + 1 else 1,
                    isLandscape = true,
                    isPlaying = snapshot.isPlaying,
                    positionMs = exoPlayer.currentPosition,
                    durationMs = durationMs,
                    onSeekTo = { exoPlayer.seekTo(it) },
                    onPrevious = {
                        PlayerEngine.skipPrevious(context)
                        landscapeControlsVisible = true
                    },
                    onTogglePlay = {
                        PlayerEngine.togglePlayPause(context)
                        landscapeControlsVisible = true
                    },
                    onNext = {
                        PlayerEngine.skipNext(context)
                        landscapeControlsVisible = true
                    },
                    onEnterFullscreen = {
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    },
                    onExitFullscreen = {
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    },
                    onToggleQueue = {
                        showQueuePanel = !showQueuePanel
                        landscapeControlsVisible = true
                    },
                    onBack = {
                        activity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    },
                    onJumpToQueueIndex = { index ->
                        PlayerEngine.jumpTo(context, index)
                        landscapeControlsVisible = true
                    }
                )
            }
        } else {
            // Portrait → controles persistentes, sin AnimatedVisibility
            VideoOverlayChrome(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f),
                currentItem = currentItem,
                queueCount = queue.size,
                queueFromCurrent = queueFromCurrent,
                queueStartIndex = if (snapshot.currentIndex in queue.indices)
                    snapshot.currentIndex + 1 else 1,
                isLandscape = false,
                isPlaying = snapshot.isPlaying,
                positionMs = exoPlayer.currentPosition,
                durationMs = durationMs,
                onSeekTo = { exoPlayer.seekTo(it) },
                onPrevious = { PlayerEngine.skipPrevious(context) },
                onTogglePlay = { PlayerEngine.togglePlayPause(context) },
                onNext = { PlayerEngine.skipNext(context) },
                onEnterFullscreen = {
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                },
                onExitFullscreen = {
                    activity?.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                },
                onToggleQueue = { showQueuePanel = !showQueuePanel },
                onBack = onClose,
                onJumpToQueueIndex = { index -> PlayerEngine.jumpTo(context, index) }
            )
        }

        // ── Panel de cola ──────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = showQueuePanel && queueFromCurrent.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            QueueDrawer(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                items = queueFromCurrent,
                startIndex = snapshot.currentIndex + 1,
                onItemClick = { absoluteIndex ->
                    PlayerEngine.jumpTo(context, absoluteIndex)
                    showQueuePanel = false
                    if (isLandscape) landscapeControlsVisible = true
                }
            )
        }
    }
}

// ── Surfaces ───────────────────────────────────────────────────────────────────

@Composable
private fun LandscapePlayerSurface(
    exoPlayer: androidx.media3.common.Player,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = modifier
    )
}

@Composable
private fun PortraitPlayerSurface(
    exoPlayer: androidx.media3.common.Player,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 4.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Box {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.15f), Color.Transparent)
                        )
                    )
            )
        }
    }
}

// ── Overlay de controles ───────────────────────────────────────────────────────

@Composable
private fun VideoOverlayChrome(
    modifier: Modifier = Modifier,
    currentItem: MediaEntry,
    queueCount: Int,
    queueFromCurrent: List<MediaEntry>,
    queueStartIndex: Int,
    isLandscape: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onEnterFullscreen: () -> Unit,
    onExitFullscreen: () -> Unit,
    onToggleQueue: () -> Unit,
    onBack: () -> Unit,
    onJumpToQueueIndex: (Int) -> Unit
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isLandscape) 0.22f else 0.08f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isLandscape) 14.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            TopChrome(
                currentItem = currentItem,
                queueCount = queueCount,
                isLandscape = isLandscape,
                onBack = onBack,
                onToggleQueue = onToggleQueue,
                onFullscreenToggle = if (isLandscape) onExitFullscreen else onEnterFullscreen
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (isLandscape) 4.dp else 12.dp))

                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier.fillMaxWidth(if (isLandscape) 0.72f else 0.96f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ActionCluster(
                            isPlaying = isPlaying,
                            onPrevious = onPrevious,
                            onTogglePlay = onTogglePlay,
                            onNext = onNext,
                            onRewind = {
                                onSeekTo((positionMs - 10_000L).coerceAtLeast(0L))
                            },
                            onForward = {
                                onSeekTo(
                                    (positionMs + 10_000L).coerceAtMost(
                                        durationMs.coerceAtLeast(positionMs + 10_000L)
                                    )
                                )
                            }
                        )

                        PlaybackSeekBar(
                            positionMs = positionMs,
                            durationMs = durationMs,
                            onSeekTo = onSeekTo,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Desliza: brillo izquierda / volumen derecha",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.72f)
                            )
                            Text(
                                text = if (isLandscape) "Pantalla completa" else "Modo normal",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                if (queueFromCurrent.isNotEmpty()) {
                    QueuePreviewRow(
                        items = queueFromCurrent.take(4),
                        startIndex = queueStartIndex,
                        onItemClick = onJumpToQueueIndex,
                        modifier = Modifier.fillMaxWidth(if (isLandscape) 0.82f else 1f)
                    )
                }
            }

            BottomChrome(
                currentItem = currentItem,
                isLandscape = isLandscape,
                positionMs = positionMs,
                durationMs = durationMs,
                onSeekTo = onSeekTo,
                onToggleQueue = onToggleQueue,
                onFullscreenToggle = if (isLandscape) onExitFullscreen else onEnterFullscreen
            )
        }
    }
}
