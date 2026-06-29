package com.nexora.player.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nexora.player.R
import com.nexora.player.data.lyrics.Lyrics
import com.nexora.player.data.lyrics.LrcParser
import com.nexora.player.data.lyrics.LyricsSource
import com.nexora.player.playback.PlayerEngine
import com.nexora.player.ui.components.formatDuration
import kotlinx.coroutines.delay
import kotlin.math.roundToLong

// ---------------------------------------------------------------------------
// Main dialog
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsEditorDialog(
    currentPositionMs: Long,
    initialText: String,
    onSave: (rawText: String, exportToFile: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context  = LocalContext.current
    val player   = PlayerEngine.get(context)
    val snapshot by PlayerEngine.snapshot.collectAsState()

    var rawText     by remember(initialText) { mutableStateOf(initialText) }
    var exportToFile by rememberSaveable { mutableStateOf(true) }
    var syncMode    by rememberSaveable { mutableStateOf(true) }

    // Live playback state — updates every 100ms for accurate sync
    var livePositionMs by remember { mutableLongStateOf(currentPositionMs) }
    var liveDurationMs by remember { mutableLongStateOf(0L) }
    var seekDragging   by remember { mutableStateOf(false) }
    var seekProgress   by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            livePositionMs = player.currentPosition.coerceAtLeast(0L)
            liveDurationMs = player.duration.takeIf { it > 0L } ?: 0L
            if (!seekDragging) {
                seekProgress = if (liveDurationMs > 0L)
                    livePositionMs.toFloat() / liveDurationMs.toFloat() else 0f
            }
            delay(100)
        }
    }

    val parsed = remember(rawText) {
        LrcParser.parse(
            rawText = rawText, mediaId = 0L,
            title = "", artist = "", album = "",
            source = LyricsSource.MANUAL
        )
    }

    val currentSong = snapshot.queue.getOrNull(snapshot.currentIndex.coerceAtLeast(0))

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color    = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Header bar ───────────────────────────────────────────────
                Surface(
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, stringResource(R.string.action_close))
                        }
                        Text(
                            stringResource(R.string.lyrics_editor_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        IconButton(onClick = { onSave(rawText, exportToFile) }) {
                            Icon(
                                Icons.Filled.Save, stringResource(R.string.action_save),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // ── Scrollable body ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // ── Mini player ──────────────────────────────────────────
                    Card(
                        shape  = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Song info + live time badge
                            Row(
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Album art placeholder
                                Surface(
                                    shape  = RoundedCornerShape(10.dp),
                                    color  = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    modifier = Modifier.size(46.dp)
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Icon(
                                            Icons.Filled.MusicNote, null,
                                            tint     = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                // Title + artist
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text     = currentSong?.title ?: stringResource(R.string.lyrics_no_song),
                                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    currentSong?.artist?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text  = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                // Live time — prominent pill
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text  = formatDuration(livePositionMs),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        ),
                                        color    = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            // Seek bar
                            Slider(
                                value            = seekProgress.coerceIn(0f, 1f),
                                onValueChange    = { seekDragging = true; seekProgress = it },
                                onValueChangeFinished = {
                                    seekDragging = false
                                    if (liveDurationMs > 0L)
                                        PlayerEngine.seekTo(
                                            context,
                                            (seekProgress * liveDurationMs).roundToLong()
                                        )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    formatDuration(livePositionMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.60f)
                                )
                                Text(
                                    formatDuration(liveDurationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.60f)
                                )
                            }

                            // Transport: prev | -5s | play/pause | +5s | next
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { PlayerEngine.skipPrevious(context) }) {
                                    Icon(Icons.Filled.SkipPrevious, stringResource(R.string.action_previous_track),
                                        modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.width(4.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        PlayerEngine.seekTo(
                                            context,
                                            (livePositionMs - 5_000L).coerceAtLeast(0L)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Filled.Replay5, "−5 seg")
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledIconButton(
                                    onClick  = { PlayerEngine.togglePlayPause(context) },
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(
                                        imageVector = if (snapshot.isPlaying) Icons.Filled.Pause
                                                      else Icons.Filled.PlayArrow,
                                        contentDescription = if (snapshot.isPlaying) stringResource(R.string.lyrics_pause) else stringResource(R.string.action_play),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                FilledTonalIconButton(
                                    onClick = {
                                        PlayerEngine.seekTo(
                                            context,
                                            (livePositionMs + 5_000L).coerceAtMost(liveDurationMs)
                                        )
                                    }
                                ) {
                                    Icon(Icons.Filled.Forward5, "+5 seg")
                                }
                                Spacer(Modifier.width(4.dp))
                                IconButton(onClick = { PlayerEngine.skipNext(context) }) {
                                    Icon(Icons.Filled.SkipNext, stringResource(R.string.action_next_track),
                                        modifier = Modifier.size(26.dp))
                                }
                            }
                        }
                    }

                    // ── Mode tabs: Sincronizar / Editar texto ────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            true  to stringResource(R.string.lyrics_sync_tab),
                            false to stringResource(R.string.lyrics_edit_text_tab)
                        ).forEach { (isSync, label) ->
                            Surface(
                                onClick  = { syncMode = isSync },
                                shape    = RoundedCornerShape(14.dp),
                                color    = if (syncMode == isSync)
                                               MaterialTheme.colorScheme.primaryContainer
                                           else
                                               MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (syncMode == isSync) FontWeight.Bold
                                                     else FontWeight.Normal
                                    ),
                                    color = if (syncMode == isSync)
                                                MaterialTheme.colorScheme.onPrimaryContainer
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier  = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 14.dp)
                                )
                            }
                        }
                    }

                    // ── Mode content ─────────────────────────────────────────
                    if (syncMode) {
                        SyncModeContent(
                            parsed        = parsed,
                            onLineTapped  = { idx -> rawText = stampLine(rawText, idx, livePositionMs) },
                            onSwitchToEdit = { syncMode = false }
                        )
                    } else {
                        EditModeContent(
                            rawText      = rawText,
                            onTextChange = { rawText = it }
                        )
                    }

                    // ── Save card ────────────────────────────────────────────
                    Card(
                        shape     = RoundedCornerShape(20.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(
                                        stringResource(R.string.lyrics_save_lrc),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Text(
                                        stringResource(R.string.lyrics_save_lrc_desc),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked         = exportToFile,
                                    onCheckedChange = { exportToFile = it }
                                )
                            }
                            Button(
                                onClick  = { onSave(rawText, exportToFile) },
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.Save, null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.lyrics_save_lyrics),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sync mode — tap each line at the right moment
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncModeContent(
    parsed: Lyrics,
    onLineTapped: (Int) -> Unit,
    onSwitchToEdit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

        // Tip card
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Text(
                text = "💡  ${stringResource(R.string.lyrics_sync_hint)}",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }

        if (parsed.lines.isEmpty()) {
            // No lines yet — guide user to Edit tab
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment   = Alignment.CenterHorizontally,
                    verticalArrangement   = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Filled.MusicNote, null,
                        modifier = Modifier.size(44.dp),
                        tint     = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.lyrics_empty_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        stringResource(R.string.lyrics_empty_body),
                        style     = MaterialTheme.typography.bodySmall,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    FilledTonalButton(onClick = onSwitchToEdit) {
                        Text(stringResource(R.string.lyrics_go_edit_text))
                    }
                }
            }
        } else {
            parsed.lines.forEachIndexed { index, line ->
                LyricSyncRow(
                    text        = line.text,
                    timestampMs = extractTimestampMs(line),
                    onTap       = { onLineTapped(index) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Edit mode — raw text area
// ---------------------------------------------------------------------------

@Composable
private fun EditModeContent(
    rawText: String,
    onTextChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Format hint
        Card(
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "💡  ${stringResource(R.string.lyrics_text_hint)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    stringResource(R.string.lyrics_example_timed),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.80f)
                )
                Text(
                    stringResource(R.string.lyrics_example_plain),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.80f)
                )
            }
        }

        OutlinedTextField(
            value          = rawText,
            onValueChange  = onTextChange,
            modifier       = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp),
            label          = { Text(stringResource(R.string.lyrics_label)) },
            placeholder    = {
                Text(
                    stringResource(R.string.lyrics_placeholder_full)
                )
            },
            shape    = RoundedCornerShape(16.dp),
            minLines = 12
        )
    }
}

// ---------------------------------------------------------------------------
// Single lyric line row for sync mode
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricSyncRow(
    text: String,
    timestampMs: Long,
    onTap: () -> Unit
) {
    val hasTime = timestampMs > 0L

    Surface(
        onClick          = onTap,
        shape            = RoundedCornerShape(14.dp),
        color            = if (hasTime) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                           else MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation   = if (hasTime) 0.dp else 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Text(
                text     = text.ifBlank { stringResource(R.string.lyrics_empty_line) },
                style    = MaterialTheme.typography.bodyLarge,
                color    = if (hasTime) MaterialTheme.colorScheme.onPrimaryContainer
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(12.dp))
            // Timestamp pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (hasTime) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            ) {
                Text(
                    text  = if (hasTime) formatDuration(timestampMs) else stringResource(R.string.lyrics_tap),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (hasTime) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = if (hasTime) FontFamily.Monospace else FontFamily.Default
                    ),
                    color    = if (hasTime) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }
    }
}


private fun extractTimestampMs(line: Any): Long {
    val getters = listOf(
        "getTimestampMs",
        "getTimeMs",
        "getTimestamp",
        "getTime"
    )

    for (getterName in getters) {
        val method = line.javaClass.methods.firstOrNull { it.name == getterName && it.parameterCount == 0 }
        val value = method?.invoke(line)
        when (value) {
            is Long -> return value
            is Int -> return value.toLong()
            is Number -> return value.toLong()
        }
    }

    return 0L
}

// ---------------------------------------------------------------------------
// Helpers (unchanged logic)
// ---------------------------------------------------------------------------

private fun stampLine(raw: String, lineIndex: Int, positionMs: Long): String {
    val lines = raw.lines().toMutableList()
    if (lineIndex !in lines.indices) return raw
    val timestamp = positionMs.toLrcTimestamp()
    val original  = lines[lineIndex].trimStart()
    val stripped  = original.replace(Regex("""^\[(\d{1,2}):(\d{2})(?:[.:]\d{1,3})?]"""), "")
    lines[lineIndex] = "[$timestamp]$stripped"
    return lines.joinToString("\n")
}

private fun Long.toLrcTimestamp(): String {
    val totalSeconds = this / 1_000L
    val minutes      = totalSeconds / 60L
    val seconds      = totalSeconds % 60L
    val centiseconds = (this % 1_000L) / 10L
    return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
}
