package com.nexora.player.presentation

class MainViewModel(
    val library: LibraryViewModel = LibraryViewModel(),
    val playback: PlaybackViewModel = PlaybackViewModel(),
    val playlists: PlaylistViewModel = PlaylistViewModel(),
    val settings: SettingsViewModel = SettingsViewModel(),
    val lyrics: LyricsViewModel = LyricsViewModel()
)
