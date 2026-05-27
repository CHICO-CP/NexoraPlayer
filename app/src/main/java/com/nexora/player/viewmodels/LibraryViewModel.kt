package com.nexora.player.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.nexora.player.models.MediaItem
import com.nexora.player.utils.MediaScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val _audioFiles = MutableLiveData<List<MediaItem>>(emptyList())
    val audioFiles: LiveData<List<MediaItem>> = _audioFiles
    private val _videoFiles = MutableLiveData<List<MediaItem>>(emptyList())
    val videoFiles: LiveData<List<MediaItem>> = _videoFiles
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init { loadMedia() }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scanner = MediaScanner(getApplication())
                withContext(Dispatchers.IO) {
                    val audio = scanner.scanAudio()
                    val video = scanner.scanVideo()
                    withContext(Dispatchers.Main) {
                        _audioFiles.value = audio
                        _videoFiles.value = video
                    }
                }
            } catch (e: Exception) {
                _audioFiles.value = emptyList()
                _videoFiles.value = emptyList()
            } finally { _isLoading.value = false }
        }
    }
}
