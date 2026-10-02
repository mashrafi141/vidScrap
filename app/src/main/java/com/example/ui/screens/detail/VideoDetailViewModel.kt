package com.example.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.Video
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

class VideoDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    private val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )

    private val _videoId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val video: StateFlow<Video?> = _videoId.flatMapLatest { id ->
        if (id != null) repository.getVideoById(id) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun loadVideo(id: Long) {
        _videoId.value = id
    }
}
