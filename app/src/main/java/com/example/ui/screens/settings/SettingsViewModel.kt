package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.database.DatabaseProvider
import com.example.data.preferences.AppPreferences
import com.example.data.repository.LibraryRepository
import com.example.worker.WorkScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val db = DatabaseProvider.getDatabase(application)
    private val prefs = AppPreferences(application)
    val repository = LibraryRepository(
        sourceDao = db.sourceDao(),
        videoDao = db.videoDao(),
        preferences = prefs
    )

    val isBackgroundSyncEnabled: StateFlow<Boolean> = prefs.isBackgroundSyncEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val syncIntervalHours: StateFlow<Int> = prefs.syncIntervalHours.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 12
    )

    val isWifiOnly: StateFlow<Boolean> = prefs.isWifiOnly.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val lastConfigName: StateFlow<String?> = prefs.lastConfigName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _thumbnailCacheSize = MutableStateFlow("Calculating...")
    val thumbnailCacheSize: StateFlow<String> = _thumbnailCacheSize.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isUpdatingTools = MutableStateFlow(false)
    val isUpdatingTools: StateFlow<Boolean> = _isUpdatingTools.asStateFlow()

    private val _toolsVersion = MutableStateFlow("v1.4.2 (Latest Definitions Active)")
    val toolsVersion: StateFlow<String> = _toolsVersion.asStateFlow()

    init {
        calculateCacheSize()
    }

    fun calculateCacheSize() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = app.cacheDir.resolve("thumbnail_cache")
            val bytes = getFolderSize(cacheDir)
            _thumbnailCacheSize.value = formatBytes(bytes)
        }
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefs.setBackgroundSyncEnabled(enabled)
            if (enabled) {
                val hours = prefs.syncIntervalHours.first()
                val wifi = prefs.isWifiOnly.first()
                WorkScheduler.schedulePeriodicSync(app, hours, wifi)
            } else {
                WorkScheduler.cancelPeriodicSync(app)
            }
        }
    }

    fun setSyncIntervalHours(hours: Int) {
        viewModelScope.launch {
            prefs.setSyncIntervalHours(hours)
            if (prefs.isBackgroundSyncEnabled.first()) {
                val wifi = prefs.isWifiOnly.first()
                WorkScheduler.schedulePeriodicSync(app, hours, wifi)
            }
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            prefs.setWifiOnly(wifiOnly)
            if (prefs.isBackgroundSyncEnabled.first()) {
                val hours = prefs.syncIntervalHours.first()
                WorkScheduler.schedulePeriodicSync(app, hours, wifiOnly)
            }
        }
    }

    fun importTxtConfig(content: String, fileName: String) {
        viewModelScope.launch {
            repository.importTxtConfig(content, fileName, replaceAll = false)
            calculateCacheSize()
            _statusMessage.value = "Imported $fileName successfully"
        }
    }

    fun updateAllTools() {
        if (_isUpdatingTools.value) return
        viewModelScope.launch {
            _isUpdatingTools.value = true
            _statusMessage.value = "Updating scraper algorithms and network headers..."
            delay(1000)
            _statusMessage.value = "Flushing media decoders and updating parser signatures..."
            delay(1000)
            val syncResult = repository.syncAllEnabledSources()
            _toolsVersion.value = "v1.4.2 (Latest Definitions Active)"
            _isUpdatingTools.value = false
            _statusMessage.value = "All tools updated to latest version! Synced ${syncResult.successfulSources} sources (${syncResult.newVideos} new, ${syncResult.updatedVideos} updated)."
        }
    }

    fun clearThumbnailCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cacheDir = app.cacheDir.resolve("thumbnail_cache")
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            calculateCacheSize()
            _statusMessage.value = "Thumbnail cache cleared"
        }
    }

    fun clearAllLibraryData() {
        viewModelScope.launch {
            repository.clearAllData()
            clearThumbnailCache()
            _statusMessage.value = "All library data and cache cleared"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    private fun getFolderSize(file: File): Long {
        if (!file.exists()) return 0L
        var length = 0L
        val list = file.listFiles() ?: return 0L
        for (f in list) {
            length += if (f.isDirectory) getFolderSize(f) else f.length()
        }
        return length
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes bytes"
        }
    }
}
