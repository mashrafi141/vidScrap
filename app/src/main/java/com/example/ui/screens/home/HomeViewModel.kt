package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.ParsedImportSummary
import com.example.core.model.Source
import com.example.core.model.Video
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LibraryRepository
import com.example.data.repository.OverallSyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class VideoSortOrder(val label: String) {
    RECENT("Recently Added"),
    TITLE("Title (A-Z)"),
    DURATION("Duration")
}

enum class HomeCategoryFilter(val label: String) {
    ALL("All Videos"),
    RECENT("Recent"),
    PLAYLISTS("Playlists"),
    PROFILES("Profiles"),
    SINGLE_VIDEOS("Single Videos")
}

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val totalCount: Int = 0,
    val isSyncing: Boolean = false,
    val syncSummary: OverallSyncResult? = null,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val sortOrder: VideoSortOrder = VideoSortOrder.RECENT,
    val showImportDialog: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(VideoSortOrder.RECENT)
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()

    private val _categoryFilter = MutableStateFlow(HomeCategoryFilter.ALL)
    val categoryFilter: StateFlow<HomeCategoryFilter> = _categoryFilter.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncSummary = MutableStateFlow<OverallSyncResult?>(null)
    val syncSummary: StateFlow<OverallSyncResult?> = _syncSummary.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showImportDialog = MutableStateFlow(false)
    val showImportDialog: StateFlow<Boolean> = _showImportDialog.asStateFlow()

    val totalCount: StateFlow<Int> = repository.totalVideoCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val allSources: StateFlow<List<Source>> = repository.allSources.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val playlistSources: StateFlow<List<Source>> = repository.getSourcesByType(com.example.core.model.SourceType.PLAYLIST).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val profileSources: StateFlow<List<Source>> = repository.getSourcesByType(com.example.core.model.SourceType.PROFILE).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val singleVideoSources: StateFlow<List<Source>> = repository.getSourcesByType(com.example.core.model.SourceType.SINGLE_VIDEO).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentVideos: StateFlow<List<Video>> = repository.getRecentVideos(8).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val displayedVideos: StateFlow<List<Video>> = combine(
        _searchQuery.flatMapLatest { query ->
            if (query.isBlank()) repository.allVideos else repository.searchVideos(query)
        },
        _sortOrder
    ) { list, sort ->
        when (sort) {
            VideoSortOrder.RECENT -> list.sortedByDescending { it.firstSeenAt }
            VideoSortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            VideoSortOrder.DURATION -> list.sortedByDescending { it.durationSeconds ?: 0L }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onSortOrderChange(order: VideoSortOrder) {
        _sortOrder.value = order
    }

    fun onCategoryFilterChange(filter: HomeCategoryFilter) {
        _categoryFilter.value = filter
    }

    fun openImportDialog() {
        _showImportDialog.value = true
    }

    fun closeImportDialog() {
        _showImportDialog.value = false
    }

    fun importTxt(content: String, configName: String) {
        viewModelScope.launch {
            try {
                repository.importTxtConfig(content, configName)
                _showImportDialog.value = false
                // Trigger sync on imported sources
                syncAllSources()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to import configuration: ${e.localizedMessage}"
            }
        }
    }

    fun syncAllSources() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                val result = repository.syncAllEnabledSources()
                _syncSummary.value = result
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Sync error occurred"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSyncSummary() {
        _syncSummary.value = null
    }
}
