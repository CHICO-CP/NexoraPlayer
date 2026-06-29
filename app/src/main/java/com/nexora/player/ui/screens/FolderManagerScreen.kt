package com.nexora.player.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.model.FolderSummary
import com.nexora.player.ui.components.formatBytes

enum class FolderManagerSort { PATH, NAME, SONGS, SIZE }

@Composable
fun FolderManagerScreen(
    modifier: Modifier = Modifier,
    folders: List<FolderSummary>,
    hiddenFolders: Set<String>,
    onBack: () -> Unit,
    onHideFolder: (String) -> Unit,
    onShowFolder: (String) -> Unit,
    onHideSmallFolders: () -> Unit,
    onHideSuggestedNoiseFolders: () -> Unit,
    onRestoreAll: () -> Unit
) {
    var sort by remember { mutableStateOf(FolderManagerSort.SONGS) }
    val sorted = remember(folders, sort) {
        when (sort) {
            FolderManagerSort.PATH -> folders.sortedBy { it.path.lowercase() }
            FolderManagerSort.NAME -> folders.sortedBy { it.name.lowercase() }
            FolderManagerSort.SONGS -> folders.sortedByDescending { it.songCount }
            FolderManagerSort.SIZE -> folders.sortedByDescending { it.totalSizeBytes }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(tonalElevation = 3.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.folder_manager_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.folders_detected, folders.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onHideSuggestedNoiseFolders) {
                    Icon(Icons.Filled.CleaningServices, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.folder_ignore_tones))
                }
                OutlinedButton(onClick = onHideSmallFolders) { Text(stringResource(R.string.folder_hide_small)) }
                OutlinedButton(onClick = onRestoreAll) {
                    Icon(Icons.Filled.Restore, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.restore))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FolderManagerSort.values().forEach { value ->
                    FilterChip(
                        selected = sort == value,
                        onClick = { sort = value },
                        label = {
                            Text(
                                when (value) {
                                    FolderManagerSort.PATH -> stringResource(R.string.folder_sort_path)
                                    FolderManagerSort.NAME -> stringResource(R.string.folder_sort_name)
                                    FolderManagerSort.SONGS -> stringResource(R.string.folder_sort_songs)
                                    FolderManagerSort.SIZE -> stringResource(R.string.folder_sort_size)
                                }
                            )
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(sorted, key = { it.path }) { folder ->
                val hidden = hiddenFolders.any { folder.path.startsWith(it) || folder.path.contains(it, ignoreCase = true) }
                FolderRow(
                    folder = folder,
                    hidden = hidden,
                    onCheckedChange = { visible ->
                        if (visible) onShowFolder(folder.path) else onHideFolder(folder.path)
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderRow(
    folder: FolderSummary,
    hidden: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(folder.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(folder.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.folder_summary, folder.songCount, formatBytes(folder.totalSizeBytes)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = !hidden, onCheckedChange = onCheckedChange)
        }
        HorizontalDivider(thickness = 0.3.dp)
    }
}
