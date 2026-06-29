package com.nexora.player.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nexora.player.R
import com.nexora.player.data.model.AppLanguage

private data class LanguageOption(
    val language: AppLanguage,
    val title: String,
    val subtitle: String,
    val badge: String,
    val color: Color
)

@Composable
fun LanguageSelectionScreen(
    modifier: Modifier = Modifier,
    selectedLanguage: AppLanguage,
    onBack: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit
) {
    val palette = listOf(
        Color(0xFF8E8E93), Color(0xFFF54047), Color(0xFF0A84FF), Color(0xFF34C759),
        Color(0xFFFF9500), Color(0xFF5856D6), Color(0xFFFF2D55), Color(0xFF30B0C7)
    )
    val options = AppLanguage.entries.mapIndexed { index, language ->
        LanguageOption(
            language = language,
            title = stringResource(language.labelRes),
            subtitle = stringResource(language.subtitleRes),
            badge = language.badge,
            color = palette[index % palette.size]
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.language_screen_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.language_screen_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(options, key = { it.language.name }) { option ->
                LanguageOptionCard(
                    option = option,
                    selected = selectedLanguage == option.language,
                    onClick = { onLanguageSelected(option.language) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    option: LanguageOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(26.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 5.dp else 2.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(option.color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(option.badge, color = option.color, fontWeight = FontWeight.Black)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(option.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.language_selected), tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
                }
            } else {
                Icon(Icons.Filled.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
