
package com.nexora.player.ui.screens

import android.content.ContentUris
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.local.FavoriteMediaEntity
import com.nexora.player.data.local.NexoraDatabase
import com.nexora.player.data.lyrics.LrcParser
import com.nexora.player.data.lyrics.Lyrics
import com.nexora.player.data.lyrics.LyricsRepository
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.model.MediaKind
import com.nexora.player.equalizer.EqualizerPreferencesRepository
import com.nexora.player.equalizer.EqualizerSessionManager
import com.nexora.player.equalizer.EqualizerSettings
import com.nexora.player.equalizer.NEXORA_CUSTOM_TEMPLATE_ID
import com.nexora.player.equalizer.NexoraEqualizerTemplates
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.LyricsAndQueueCard
import com.nexora.player.ui.components.MediaItemRow
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToLong

private enum class ArtworkStyle {
    DISC,
    COVER,
    SQUARE;

    fun next(): ArtworkStyle = when (this) {
        DISC -> COVER
        COVER -> SQUARE
        SQUARE -> DISC
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioPlayerScreen(
    modifier: Modifier = Modifier,
    current: MediaEntry?,
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val player = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()
    val scope = rememberCoroutineScope()

    val db = remember(context) { NexoraDatabase.get(context) }
    val favorites by db.favoritesDao().observeAll().collectAsState(initial = emptyList())
    val lyricsRepository = remember(context) { LyricsRepository(context, db) }
    val equalizerRepository = remember(context) { EqualizerPreferencesRepository(context) }
    val equalizerSettings by equalizerRepository.settings.collectAsState(initial = EqualizerSettings())

    val equalizerPresetLabel = remember(equalizerSettings.templateId, equalizerSettings.customName) {
        if (equalizerSettings.templateId == NEXORA_CUSTOM_TEMPLATE_ID) {
            equalizerSettings.customName.trim().ifBlank { "Personalizado" }
        } else {
            NexoraEqualizerTemplates.resolve(equalizerSettings.templateId).name
        }
    }

    var lyrics by remember { mutableStateOf<Lyrics?>(null) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var allowOnlineLyrics by rememberSaveable(current?.id) { mutableStateOf(false) }
    var showLyricsEditor by rememberSaveable(current?.id) { mutableStateOf(false) }
    var showQueueSheet by rememberSaveable(current?.id) { mutableStateOf(false) }
    var showDetailsSheet by rememberSaveable(current?.id) { mutableStateOf(false) }
    var showEqualizerSheet by rememberSaveable(current?.id) { mutableStateOf(false) }
    var artworkStyle by rememberSaveable(current?.id) { mutableStateOf(ArtworkStyle.DISC) }

    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }

    val artwork by produceState<ImageBitmap?>(
        initialValue = null,
        key1 = current?.id,
        key2 = current?.uri?.toString()
    ) {
        value = withContext(Dispatchers.IO) {
            current?.let { loadArtworkBitmap(context, it)?.asImageBitmap() }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "audio_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isFavorite = remember(current?.id, current?.kind, favorites) {
        val item = current
        item != null && favorites.any { fav -> fav.mediaId == item.id && fav.mediaKind == item.kind.name }
    }

    LaunchedEffect(player.audioSessionId, equalizerSettings) {
        if (player.audioSessionId > 0) {
            EqualizerSessionManager.sync(player.audioSessionId, equalizerSettings)
        }
    }

    LaunchedEffect(current?.id, snapshot.isPlaying) {
        if (current == null) return@LaunchedEffect
        while (true) {
            positionMs = player.currentPosition.coerceAtLeast(0L)
            durationMs = player.duration.takeIf { it > 0L } ?: current.durationMs
            delay(350)
        }
    }

    LaunchedEffect(current?.id, allowOnlineLyrics) {
        val item = current
        if (item == null) {
            lyrics = null
            lyricsLoading = false
            return@LaunchedEffect
        }
        lyricsLoading = true
        lyrics = runCatching { lyricsRepository.loadLyrics(item, allowOnlineSearch = allowOnlineLyrics) }.getOrNull()
        lyricsLoading = false
    }

    val queue = snapshot.queue
    val currentIndex = snapshot.currentIndex.coerceAtLeast(0)
    val upNext = if (queue.isEmpty()) emptyList() else queue.drop(currentIndex + 1)

    BackHandler { onClose() }

    if (current == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF090B14)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.audio_no_playback),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090B14))
    ) {
        if (artwork != null) {
            Image(
                bitmap = artwork!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(42.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFF54047).copy(alpha = 0.40f),
                                Color(0xFF090B14).copy(alpha = 0.94f)
                            )
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.60f),
                            Color.Black.copy(alpha = 0.94f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TopHeader(
                current = current,
                isFavorite = isFavorite,
                onBack = onClose,
                onToggleFavorite = {
                    scope.launch {
                        toggleFavorite(context, current)
                    }
                },
                onOpenDetails = { showDetailsSheet = true }
            )

            ArtworkDisplay(
                artwork = artwork,
                title = current.title,
                isPlaying = snapshot.isPlaying,
                rotation = rotation,
                style = artworkStyle,
                modifier = Modifier.fillMaxWidth()
            )

            NowPlayingSummary(
                current = current,
                equalizerPresetLabel = equalizerPresetLabel,
                durationMs = durationMs,
                artworkStyle = artworkStyle,
                onCycleArtworkStyle = { artworkStyle = artworkStyle.next() }
            )

            PlaybackProgressSection(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = { PlayerEngine.seekTo(context, it) }
            )

            TransportControls(
                isPlaying = snapshot.isPlaying,
                onPrevious = { PlayerEngine.skipPrevious(context) },
                onTogglePlay = { PlayerEngine.togglePlayPause(context) },
                onNext = { PlayerEngine.skipNext(context) }
            )

            ActionStrip(
                onQueue = { showQueueSheet = true },
                onDetails = { showDetailsSheet = true },
                onEqualizer = { showEqualizerSheet = true },
                onLyrics = { showLyricsEditor = true }
            )

            LyricsAndQueueCard(
                lyrics = lyrics,
                lyricsLoading = lyricsLoading,
                positionMs = positionMs,
                queue = queue,
                currentIndex = currentIndex,
                onJumpToQueueItem = { PlayerEngine.jumpTo(context, it) },
                onSearchOnline = {
                    allowOnlineLyrics = true
                },
                onEditManual = {
                    showLyricsEditor = true
                },
                onShowQueue = {
                    showQueueSheet = true
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showQueueSheet) {
        QueueSheet(
            queue = queue,
            currentIndex = currentIndex,
            onDismiss = { showQueueSheet = false },
            onJumpTo = {
                PlayerEngine.jumpTo(context, it)
                showQueueSheet = false
            }
        )
    }

    if (showDetailsSheet) {
        DetailsSheet(
            current = current,
            durationMs = durationMs,
            equalizerPresetLabel = equalizerPresetLabel,
            artworkStyle = artworkStyle,
            allowOnlineLyrics = allowOnlineLyrics,
            onToggleOnlineLyrics = { allowOnlineLyrics = it },
            onCycleArtworkStyle = { artworkStyle = artworkStyle.next() },
            onOpenEqualizer = {
                showDetailsSheet = false
                showEqualizerSheet = true
            },
            onDismiss = { showDetailsSheet = false }
        )
    }

    if (showEqualizerSheet) {
        EqualizerSheet(
            audioSessionId = player.audioSessionId,
            onDismiss = { showEqualizerSheet = false }
        )
    }

    if (showLyricsEditor && current != null) {
        LyricsEditorDialog(
            currentPositionMs = positionMs,
            initialText = lyrics?.rawText.orEmpty(),
            onSave = { rawText, exportToFile ->
                scope.launch {
                    val parsed = LrcParser.parse(
                        rawText = rawText,
                        mediaId = current.id,
                        title = current.title,
                        artist = current.artist,
                        album = current.album,
                        source = com.nexora.player.data.lyrics.LyricsSource.MANUAL
                    )
                    lyricsRepository.saveLyrics(current, parsed, exportToSidecarFile = exportToFile)
                    lyrics = parsed
                    showLyricsEditor = false
                }
            },
            onDismiss = { showLyricsEditor = false }
        )
    }
}

@Composable
private fun TopHeader(
    current: MediaEntry,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenDetails: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Now Playing",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (current.kind == MediaKind.VIDEO) "Video" else "Audio",
                color = Color.White.copy(alpha = 0.68f),
                style = MaterialTheme.typography.labelMedium
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) Color(0xFFF54047) else Color.White
                )
            }
            IconButton(onClick = onOpenDetails) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun NowPlayingSummary(
    current: MediaEntry,
    equalizerPresetLabel: String,
    durationMs: Long,
    artworkStyle: ArtworkStyle,
    onCycleArtworkStyle: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = buildList {
                    if (current.artist.isNotBlank()) add(current.artist)
                    if (current.album.isNotBlank()) add(current.album)
                }.joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(if (current.kind == MediaKind.VIDEO) "Video" else "Audio") })
                AssistChip(onClick = {}, label = { Text(equalizerPresetLabel) }, leadingIcon = { Icon(Icons.Filled.Equalizer, null) })
                AssistChip(onClick = onCycleArtworkStyle, label = { Text(styleLabel(artworkStyle)) }, leadingIcon = { Icon(Icons.Filled.QueueMusic, null) })
            }

            Text(
                text = formatDuration(durationMs),
                color = Color.White.copy(alpha = 0.62f),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

@Composable
private fun ActionStrip(
    onQueue: () -> Unit,
    onDetails: () -> Unit,
    onEqualizer: () -> Unit,
    onLyrics: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ActionChip(label = "Cola", icon = Icons.Filled.QueueMusic, onClick = onQueue, modifier = Modifier.weight(1f))
        ActionChip(label = "Letras", icon = Icons.AutoMirrored.Filled.PlaylistPlay, onClick = onLyrics, modifier = Modifier.weight(1f))
        ActionChip(label = "EQ", icon = Icons.Filled.Equalizer, onClick = onEqualizer, modifier = Modifier.weight(1f))
        ActionChip(label = "Detalles", icon = Icons.Filled.Info, onClick = onDetails, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun ArtworkDisplay(
    artwork: ImageBitmap?,
    title: String,
    isPlaying: Boolean,
    rotation: Float,
    style: ArtworkStyle,
    modifier: Modifier = Modifier
) {
    val outerShape = when (style) {
        ArtworkStyle.DISC -> CircleShape
        ArtworkStyle.COVER -> RoundedCornerShape(40.dp)
        ArtworkStyle.SQUARE -> RoundedCornerShape(28.dp)
    }
    val outerSize = when (style) {
        ArtworkStyle.DISC -> 318.dp
        ArtworkStyle.COVER -> 314.dp
        ArtworkStyle.SQUARE -> 300.dp
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(outerSize)
                .shadow(34.dp, outerShape, clip = false)
                .clip(outerShape)
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.12f), outerShape)
        ) {
            if (artwork != null) {
                Image(
                    bitmap = artwork,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(rotationZ = if (style == ArtworkStyle.DISC && isPlaying) rotation else 0f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF54047).copy(alpha = 0.32f),
                                    Color.Black.copy(alpha = 0.20f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(92.dp)
                    )
                }
            }

            if (style == ArtworkStyle.DISC) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.26f))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(14.dp)
                    .background(Color.Black.copy(alpha = 0.28f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = styleLabel(style),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun PlaybackProgressSection(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val progress = (positionMs.coerceIn(0L, safeDuration).toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Slider(
                value = progress,
                onValueChange = { onSeek((safeDuration * it).roundToLong()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color(0xFFF54047),
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatDuration(positionMs), color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.labelMedium)
                Text(formatDuration(durationMs), color = Color.White.copy(alpha = 0.74f), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    onPrevious: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalIconButton(onClick = onPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = stringResource(R.string.player_previous))
        }
        Spacer(modifier = Modifier.width(18.dp))
        FilledTonalIconButton(onClick = onTogglePlay) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.player_pause) else stringResource(R.string.player_play)
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        FilledTonalIconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = stringResource(R.string.player_next))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QueueSheet(
    queue: List<MediaEntry>,
    currentIndex: Int,
    onDismiss: () -> Unit,
    onJumpTo: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Cola de reproducción", style = MaterialTheme.typography.titleLarge)
                    Text("Selecciona un elemento para saltar directamente.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null) }
            }

            HorizontalDivider()

            if (queue.isEmpty()) {
                Text("No hay elementos en cola.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.heightIn(max = 420.dp)) {
                    queue.forEachIndexed { index, item ->
                        val highlighted = index == currentIndex
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.outlinedCardColors(containerColor = if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                        ) {
                            MediaItemRow(
                                item = item,
                                onClick = { onJumpTo(index) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailsSheet(
    current: MediaEntry,
    durationMs: Long,
    equalizerPresetLabel: String,
    artworkStyle: ArtworkStyle,
    allowOnlineLyrics: Boolean,
    onToggleOnlineLyrics: (Boolean) -> Unit,
    onCycleArtworkStyle: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = listOfNotNull(current.artist.takeIf { it.isNotBlank() }, current.album.takeIf { it.isNotBlank() }).joinToString(" • ").ifBlank { "Detalles de reproducción" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = null) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                AssistChip(onClick = onOpenEqualizer, label = { Text(equalizerPresetLabel) }, leadingIcon = { Icon(Icons.Filled.Equalizer, null) })
                AssistChip(onClick = onCycleArtworkStyle, label = { Text(styleLabel(artworkStyle)) }, leadingIcon = { Icon(Icons.Filled.QueueMusic, null) })
            }

            ListItem(
                headlineContent = { Text("Letras online") },
                supportingContent = { Text("Permite buscar coincidencias en línea cuando no hay letras locales.") },
                trailingContent = {
                    Switch(
                        checked = allowOnlineLyrics,
                        onCheckedChange = onToggleOnlineLyrics
                    )
                }
            )

            HorizontalDivider()

            DetailsRow("Duración", formatDuration(durationMs))
            DetailsRow("Tipo", if (current.kind == MediaKind.VIDEO) "Video" else "Audio")
            DetailsRow("Resolución", current.resolutionLabel)
            DetailsRow("Carpeta", current.folder.orEmpty().ifBlank { "—" })
            DetailsRow("Álbum", current.album.ifBlank { "—" })
            DetailsRow("Artista", current.artist.ifBlank { "—" })
        }
    }
}

@Composable
private fun DetailsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun styleLabel(style: ArtworkStyle): String = when (style) {
    ArtworkStyle.DISC -> "Disco"
    ArtworkStyle.COVER -> "Portada"
    ArtworkStyle.SQUARE -> "Cuadrado"
}

private suspend fun toggleFavorite(context: Context, entry: MediaEntry?) {
    if (entry == null) return
    if (entry.kind == MediaKind.VIDEO) return

    val db = NexoraDatabase.get(context)
    val exists = db.favoritesDao().isFavorite(entry.id, entry.kind.name)
    if (exists) {
        db.favoritesDao().delete(entry.id, entry.kind.name)
    } else {
        db.favoritesDao().upsert(
            FavoriteMediaEntity(
                mediaId = entry.id,
                mediaKind = entry.kind.name,
                title = entry.title,
                artist = entry.artist,
                album = entry.album,
                durationMs = entry.durationMs,
                uriString = entry.uri.toString()
            )
        )
    }
}

private fun loadArtworkBitmap(context: Context, item: MediaEntry): Bitmap? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, item.uri)
        retriever.embeddedPicture?.let { bytes ->
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } ?: loadAlbumArtBitmap(context, item.albumId)
    } catch (_: Throwable) {
        loadAlbumArtBitmap(context, item.albumId)
    } finally {
        runCatching { retriever.release() }
    }
}

private fun loadAlbumArtBitmap(context: Context, albumId: Long?): Bitmap? {
    if (albumId == null || albumId <= 0L) return null
    return runCatching {
        val albumUri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)
        context.contentResolver.query(
            albumUri,
            arrayOf(MediaStore.Audio.Albums.ALBUM_ART),
            null,
            null,
            null
        )?.use { cursor ->
            val artCol = cursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
            if (cursor.moveToFirst() && artCol >= 0) {
                val path = cursor.getString(artCol)
                if (!path.isNullOrBlank()) BitmapFactory.decodeFile(path) else null
            } else null
        }
    }.getOrNull()
}

private fun findComponentActivity(context: Context): ComponentActivity? = when (context) {
    is ComponentActivity -> context
    is ContextWrapper -> findComponentActivity(context.baseContext)
    else -> null
}
