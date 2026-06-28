package com.nexora.player.data.share

import com.nexora.player.BuildConfig

object NexoraShareText {
    fun build(downloadUrl: String = BuildConfig.NEXORA_SERVER_URL): String {
        val safeUrl = downloadUrl.ifBlank { BuildConfig.NEXORA_SERVER_URL }
        return """
            🎧 Descubre Nexora Player

            Un reproductor multimedia moderno para Android, diseñado para escuchar tu música y ver tus videos con una experiencia más limpia, rápida y elegante.

            ✨ Funciones destacadas:
            🎵 Reproduce música y videos locales
            🌙 Interfaz moderna con diseño premium
            🔀 Modo aleatorio y repetición
            ⏱️ Sleep Timer para dormir escuchando música
            🎚️ Crossfade entre canciones
            📁 Organización por carpetas, álbumes y artistas
            📝 Letras de canciones con traducción
            ⭐ Playlists y lista automática de más escuchadas
            📲 Actualizaciones desde la app

            Si quieres un reproductor completo, ligero y con estilo, prueba Nexora Player.

            ⬇️ Descárgalo desde la página oficial:
            $safeUrl
        """.trimIndent()
    }
}
