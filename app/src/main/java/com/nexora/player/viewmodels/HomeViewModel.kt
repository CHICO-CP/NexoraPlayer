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

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val _recentMedia = MutableLiveData<List<MediaItem>>(emptyList())
    val recentMedia: LiveData<List<MediaItem>> = _recentMedia
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    init { loadRecentMedia() }

    fun loadRecentMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val scanner = MediaScanner(getApplication())
                val media = withContext(Dispatchers.IO) {
                    (scanner.scanAudio() + scanner.scanVideo())
                        .sortedByDescending { it.dateAdded }.take(20)
                }
                _recentMedia.value = media
            } catch (e: Exception) {
                _recentMedia.value = emptyList()
            } finally { _isLoading.value = false }
        }
    }
}
