package com.example.ui.screens.source

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ScrapeResult
import com.example.core.model.Source
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
import kotlinx.coroutines.launch

class SourceVideosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    private val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )

    private val _sourceId = MutableStateFlow<Long?>(null)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val source: StateFlow<Source?> = _sourceId.flatMapLatest { id ->
        if (id != null) repository.getSourceById(id) else flowOf(null)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val videos: StateFlow<List<Video>> = _sourceId.flatMapLatest { id ->
        if (id != null) repository.getVideosForSource(id) else flowOf(emptyList())
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun loadSource(sourceId: Long) {
        _sourceId.value = sourceId
    }

    fun syncCurrentSource() {
        val id = _sourceId.value ?: return
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _statusMessage.value = null
            when (val res = repository.syncSource(id)) {
                is ScrapeResult.Success -> {
                    _statusMessage.value = "Synced: ${res.newVideosCount} new, ${res.updatedVideosCount} updated"
                }
                is ScrapeResult.Error -> {
                    _statusMessage.value = "Sync error: ${res.message}"
                }
            }
            _isSyncing.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
