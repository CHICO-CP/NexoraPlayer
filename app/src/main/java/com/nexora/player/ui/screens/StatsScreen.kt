package com.nexora.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.local.PlaybackStatsEntity
import com.nexora.player.data.model.MediaKind

@Composable
fun StatsScreen(
    modifier: Modifier = Modifier,
    stats: List<PlaybackStatsEntity>,
    onBack: () -> Unit
) {
    val unknownArtist = stringResource(R.string.unknown_artist)
    val noAlbum = stringResource(R.string.no_album)
    val noData = stringResource(R.string.stats_no_data)
    val audioStats = stats.filter { it.mediaKind == MediaKind.AUDIO.name }
    val videoStats = stats.filter { it.mediaKind == MediaKind.VIDEO.name }
    val topSong = audioStats.maxByOrNull { it.playCount }
    val topVideo = videoStats.maxByOrNull { it.playCount }
    val topArtist = audioStats.groupBy { it.artist.ifBlank { unknownArtist } }
        .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }
    val topAlbum = audioStats.groupBy { it.album.ifBlank { noAlbum } }
        .maxByOrNull { entry -> entry.value.sumOf { it.playCount } }
    val minutes = (stats.sumOf { it.durationMs.coerceAtLeast(0L) * it.playCount } / 60000L).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
            Column {
                Text(stringResource(R.string.stats_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.stats_subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatTile(stringResource(R.string.stats_minutes), "$minutes", stringResource(R.string.stats_played), modifier = Modifier.weight(1f))
                    StatTile(stringResource(R.string.stats_items), "${stats.size}", stringResource(R.string.stats_registered), modifier = Modifier.weight(1f))
                }
            }
            item { FeaturedStat(stringResource(R.string.stats_top_song), topSong?.title ?: noData, topSong?.artist.orEmpty(), Icons.Filled.MusicNote) }
            item { FeaturedStat(stringResource(R.string.stats_top_artist), topArtist?.key ?: noData, stringResource(R.string.stats_play_count, topArtist?.value?.sumOf { it.playCount } ?: 0), Icons.Filled.Person) }
            item { FeaturedStat(stringResource(R.string.stats_top_album), topAlbum?.key ?: noData, stringResource(R.string.stats_play_count, topAlbum?.value?.sumOf { it.playCount } ?: 0), Icons.Filled.Album) }
            item { FeaturedStat(stringResource(R.string.stats_top_video), topVideo?.title ?: noData, stringResource(R.string.stats_play_count, topVideo?.playCount ?: 0), Icons.Filled.Movie) }
            item {
                Text(stringResource(R.string.stats_weekly_monthly), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
                Text(stringResource(R.string.stats_data_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(stats.take(25), key = { it.mediaKey }) { item ->
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (item.mediaKind == MediaKind.VIDEO.name) Icons.Filled.Movie else Icons.Filled.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                            Text(listOf(item.artist, item.album).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { item.mediaKind }, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${item.playCount}x", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatTile(title: String, value: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.11f), modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Filled.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Text(title, style = MaterialTheme.typography.labelLarge)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FeaturedStat(title: String, value: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
