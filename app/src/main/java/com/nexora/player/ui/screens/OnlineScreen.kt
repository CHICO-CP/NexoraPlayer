package com.nexora.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nexora.player.R
import com.nexora.player.data.model.MediaEntry
import com.nexora.player.data.online.OnlineSongDto
import com.nexora.player.data.online.OnlineUiState
import com.nexora.player.ui.components.formatDuration

@Composable
fun OnlineScreen(
    modifier: Modifier = Modifier,
    state: OnlineUiState,
    localAudio: List<MediaEntry>,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGoogleLogin: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onPlaySong: (OnlineSongDto) -> Unit,
    onToggleUploadSelection: (MediaEntry) -> Unit,
    onClearUploadSelection: () -> Unit,
    onUploadSelected: () -> Unit
) {
    if (state.restoringSession) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CircularProgressIndicator()
                Text(stringResource(R.string.online_restoring_session))
            }
        }
        return
    }

    if (!state.loggedIn) {
        OnlineAuthContent(
            modifier = modifier,
            loading = state.authLoading,
            error = state.authError,
            onLogin = onLogin,
            onRegister = onRegister,
            onGoogleLogin = onGoogleLogin
        )
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        R.string.online_tab_home,
        R.string.online_tab_search,
        R.string.online_tab_account,
        R.string.online_tab_upload
    )

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { index, titleRes ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(stringResource(titleRes), maxLines = 1) }
                )
            }
        }

        when (tab) {
            0 -> OnlineHomeContent(
                state = state,
                onRefresh = onRefresh,
                onPlaySong = onPlaySong,
                modifier = Modifier.fillMaxSize()
            )
            1 -> OnlineSearchContent(
                state = state,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                onClearSearch = onClearSearch,
                onPlaySong = onPlaySong,
                modifier = Modifier.fillMaxSize()
            )
            2 -> OnlineAccountContent(
                state = state,
                onLogout = onLogout,
                modifier = Modifier.fillMaxSize()
            )
            3 -> OnlineUploadContent(
                localAudio = localAudio,
                selectedIds = state.selectedUploadIds,
                progress = state.uploadProgress,
                onToggleSelection = onToggleUploadSelection,
                onClearSelection = onClearUploadSelection,
                onUploadSelected = onUploadSelected,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun OnlineAuthContent(
    modifier: Modifier,
    loading: Boolean,
    error: String?,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String, String) -> Unit,
    onGoogleLogin: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var registerMode by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OnlineHeaderCard(
                title = stringResource(R.string.online_auth_title),
                subtitle = stringResource(R.string.online_auth_subtitle),
                icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(34.dp)) }
            )
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(28.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (registerMode) stringResource(R.string.online_create_account) else stringResource(R.string.online_login),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(
                        enabled = !loading,
                        onClick = onGoogleLogin,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.online_google_login_action))
                    }
                    Text(
                        text = stringResource(R.string.online_or_email_login),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.online_email)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.online_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (registerMode) {
                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it },
                            label = { Text(stringResource(R.string.online_username)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!error.isNullOrBlank()) {
                        ErrorCard(message = error)
                    }
                    Button(
                        enabled = !loading && email.isNotBlank() && password.length >= 6 && (!registerMode || username.length >= 3),
                        onClick = {
                            if (registerMode) onRegister(email, password, username) else onLogin(email, password)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (registerMode) stringResource(R.string.online_register_action) else stringResource(R.string.online_login_action))
                        }
                    }
                    TextButton(onClick = { registerMode = !registerMode }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (registerMode) stringResource(R.string.online_have_account) else stringResource(R.string.online_need_account))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineHomeContent(
    state: OnlineUiState,
    onRefresh: () -> Unit,
    onPlaySong: (OnlineSongDto) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OnlineHeaderCard(
                title = stringResource(R.string.online_home_title),
                subtitle = stringResource(R.string.online_home_subtitle),
                icon = { Icon(Icons.Filled.CloudDone, contentDescription = null, modifier = Modifier.size(34.dp)) },
                trailing = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_refresh))
                    }
                }
            )
        }
        if (state.loadingSongs) {
            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        }
        state.songsError?.let { item { ErrorCard(message = it, onRetry = onRefresh) } }
        if (!state.loadingSongs && state.songs.isEmpty() && state.songsError == null) {
            item { EmptyOnlineCard(text = stringResource(R.string.online_empty_home)) }
        }
        items(state.songs, key = { it.id }) { song ->
            OnlineSongRow(song = song, onClick = { onPlaySong(song) })
        }
    }
}

@Composable
private fun OnlineSearchContent(
    state: OnlineUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onPlaySong: (OnlineSongDto) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.onlineQuery,
                            onValueChange = onQueryChange,
                            label = { Text(stringResource(R.string.online_search_hint)) },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onSearch) {
                            Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.online_search_action))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onClearSearch, label = { Text(stringResource(R.string.online_clear_search)) })
                        AssistChip(onClick = onSearch, label = { Text(stringResource(R.string.online_search_action)) })
                    }
                }
            }
        }
        if (state.searching) item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
        state.searchError?.let { item { ErrorCard(message = it, onRetry = onSearch) } }
        if (state.onlineQuery.isNotBlank() && !state.searching && state.searchResults.isEmpty() && state.searchError == null) {
            item { EmptyOnlineCard(text = stringResource(R.string.online_empty_search)) }
        }
        items(state.searchResults, key = { it.id }) { song ->
            OnlineSongRow(song = song, onClick = { onPlaySong(song) })
        }
    }
}

@Composable
private fun OnlineAccountContent(
    state: OnlineUiState,
    onLogout: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            OnlineHeaderCard(
                title = stringResource(R.string.online_account_title),
                subtitle = state.session?.email ?: stringResource(R.string.online_account_connected),
                icon = { Icon(Icons.Filled.AccountCircle, contentDescription = null, modifier = Modifier.size(34.dp)) }
            )
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.online_account_session_persistent), style = MaterialTheme.typography.bodyMedium)
                    FilledTonalButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Logout, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.online_logout))
                    }
                }
            }
        }
    }
}

@Composable
private fun OnlineUploadContent(
    localAudio: List<MediaEntry>,
    selectedIds: Set<Long>,
    progress: com.nexora.player.data.online.OnlineUploadProgress,
    onToggleSelection: (MediaEntry) -> Unit,
    onClearSelection: () -> Unit,
    onUploadSelected: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OnlineHeaderCard(
                title = stringResource(R.string.online_upload_title),
                subtitle = stringResource(R.string.online_upload_subtitle),
                icon = { Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(34.dp)) }
            )
        }
        item {
            ElevatedCard(shape = RoundedCornerShape(24.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.online_upload_selected, selectedIds.size), fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onUploadSelected,
                            enabled = selectedIds.isNotEmpty() && !progress.running,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.online_upload_selected_action)) }
                        FilledTonalButton(
                            onClick = onClearSelection,
                            enabled = selectedIds.isNotEmpty() && !progress.running,
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.online_clear_selection)) }
                    }
                    if (progress.running) {
                        LinearProgressIndicator(
                            progress = { if (progress.total > 0) progress.currentIndex / progress.total.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    progress.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    progress.errors.take(3).forEach { ErrorCard(message = it) }
                }
            }
        }
        if (localAudio.isEmpty()) {
            item { EmptyOnlineCard(text = stringResource(R.string.online_upload_empty_local)) }
        }
        items(localAudio, key = { it.id }) { item ->
            UploadableLocalSongRow(
                item = item,
                selected = item.id in selectedIds,
                enabled = !progress.running,
                onToggle = { onToggleSelection(item) }
            )
        }
    }
}

@Composable
private fun OnlineHeaderCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.56f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) { icon() }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun OnlineSongRow(song: OnlineSongDto, onClick: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), MaterialTheme.colorScheme.surfaceVariant))
                ),
                contentAlignment = Alignment.Center
            ) {
                if (!song.coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = song.coverUrl,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(song.artist, song.album).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { stringResource(R.string.online_unknown_artist) },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatDuration((song.durationSeconds ?: 0L) * 1000L),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onClick) {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.action_play))
            }
        }
    }
}

@Composable
private fun UploadableLocalSongRow(
    item: MediaEntry,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onToggle).padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = enabled)
            Box(
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.LibraryMusic, contentDescription = null)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(
                    item.artist.ifBlank { item.album }.ifBlank { formatDuration(item.durationMs) },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        HorizontalDivider(thickness = 0.4.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            if (onRetry != null) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
            }
        }
    }
}

@Composable
private fun EmptyOnlineCard(text: String) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, modifier = Modifier.size(34.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
