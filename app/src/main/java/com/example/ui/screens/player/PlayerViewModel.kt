package com.example.ui.screens.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.Video
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.remote.scraper.PlaybackResolution
import com.example.data.remote.scraper.PlaybackResolver
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

sealed class PlayerState {
    data object Loading : PlayerState()
    data class Ready(val streamUrl: String, val isHls: Boolean, val webUrl: String) : PlayerState()
    data class WebPlayer(val webUrl: String, val title: String) : PlayerState()
    data class Error(val message: String, val webUrl: String) : PlayerState()
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    private val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )
    private val resolver = PlaybackResolver()

    private val _video = MutableStateFlow<Video?>(null)
    val video: StateFlow<Video?> = _video.asStateFlow()

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Loading)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun loadAndResolve(videoId: Long) {
        viewModelScope.launch {
            _playerState.value = PlayerState.Loading
            val item = repository.getVideoById(videoId).firstOrNull()
            if (item == null) {
                _playerState.value = PlayerState.Error("Video not found in local library", "")
                return@launch
            }

            _video.value = item

            when (val resolution = resolver.resolve(item)) {
                is PlaybackResolution.Playable -> {
                    _playerState.value = PlayerState.Ready(
                        streamUrl = resolution.streamUrl,
                        isHls = resolution.isHls,
                        webUrl = item.pageUrl
                    )
                }
                is PlaybackResolution.Unavailable -> {
                    // Automatically fall back to WebPlayer if direct stream cannot be resolved
                    _playerState.value = PlayerState.WebPlayer(
                        webUrl = item.pageUrl,
                        title = item.title
                    )
                }
            }
        }
    }

    fun switchToWebPlayer() {
        val currentVideo = _video.value ?: return
        _playerState.value = PlayerState.WebPlayer(
            webUrl = currentVideo.pageUrl,
            title = currentVideo.title
        )
    }

    fun handlePlayerError(errorMessage: String) {
        val currentVideo = _video.value
        _playerState.value = PlayerState.Error(
            message = errorMessage,
            webUrl = currentVideo?.pageUrl ?: ""
        )
    }

    fun retry(videoId: Long) {
        loadAndResolve(videoId)
    }
}
