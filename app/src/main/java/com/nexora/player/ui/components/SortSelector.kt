package com.nexora.player.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.nexora.player.data.model.SortMode

@Composable
fun SortSelector(
    selected: SortMode,
    options: List<SortMode>,
    onSelected: (SortMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.label())
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label()) },
                    onClick = {
                        expanded = false
                        onSelected(mode)
                    }
                )
            }
        }
    }
}

private fun SortMode.label(): String = when (this) {
    SortMode.DATE_ADDED_DESC -> "Fecha: recientes"
    SortMode.DATE_ADDED_ASC -> "Fecha: antiguos"
    SortMode.TITLE_ASC -> "Título A-Z"
    SortMode.TITLE_DESC -> "Título Z-A"
    SortMode.DURATION_ASC -> "Duración: corta"
    SortMode.DURATION_DESC -> "Duración: larga"
    SortMode.ARTIST_ASC -> "Artista"
    SortMode.ALBUM_ASC -> "Álbum"
    SortMode.SIZE_ASC -> "Tamaño: pequeño"
    SortMode.SIZE_DESC -> "Tamaño: grande"
    SortMode.RESOLUTION_ASC -> "Resolución: baja"
    SortMode.RESOLUTION_DESC -> "Resolución: alta"
}
