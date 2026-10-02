package com.example.ui.screens.sources

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ScrapeResult
import com.example.core.model.Source
import com.example.core.model.SourceType
import com.example.core.model.Video
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SourcesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    private val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )

    private val _selectedTab = MutableStateFlow(SourceType.PLAYLIST)
    val selectedTab: StateFlow<SourceType> = _selectedTab.asStateFlow()

    private val _syncingSourceId = MutableStateFlow<Long?>(null)
    val syncingSourceId: StateFlow<Long?> = _syncingSourceId.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val sources: StateFlow<List<Source>> = _selectedTab.flatMapLatest { tab ->
        repository.getSourcesByType(tab)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val singleVideos: StateFlow<List<Video>> = repository.getVideosBySourceType(SourceType.SINGLE_VIDEO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onTabSelected(type: SourceType) {
        _selectedTab.value = type
    }

    fun syncSource(sourceId: Long) {
        if (_syncingSourceId.value != null) return
        viewModelScope.launch {
            _syncingSourceId.value = sourceId
            _statusMessage.value = null
            when (val res = repository.syncSource(sourceId)) {
                is ScrapeResult.Success -> {
                    _statusMessage.value = "Synced successfully: ${res.newVideosCount} new, ${res.updatedVideosCount} updated"
                }
                is ScrapeResult.Error -> {
                    _statusMessage.value = "Sync failed: ${res.message}"
                }
            }
            _syncingSourceId.value = null
        }
    }

    fun deleteSource(sourceId: Long) {
        viewModelScope.launch {
            repository.deleteSource(sourceId)
        }
    }

    fun toggleSourceEnabled(sourceId: Long, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleSourceEnabled(sourceId, enabled)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}
