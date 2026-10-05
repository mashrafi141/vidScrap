package com.example.data.repository

import com.example.core.model.ParsedImportSummary
import com.example.core.model.ScrapeResult
import com.example.core.model.Source
import com.example.core.model.SourceType
import com.example.core.model.SyncStatus
import com.example.core.model.Video
import com.example.core.util.TxtConfigParser
import com.example.core.util.UrlUtils
import com.example.data.local.dao.SourceDao
import com.example.data.local.dao.VideoDao
import com.example.data.local.entity.SourceEntity
import com.example.data.local.entity.SourceVideoCrossRef
import com.example.data.local.entity.VideoEntity
import com.example.data.preferences.AppPreferences
import com.example.data.remote.scraper.ScraperEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class OverallSyncResult(
    val totalSources: Int,
    val successfulSources: Int,
    val failedSources: Int,
    val newVideos: Int,
    val updatedVideos: Int
)

class LibraryRepository(
    private val sourceDao: SourceDao,
    private val videoDao: VideoDao,
    private val scraperEngine: ScraperEngine = ScraperEngine(),
    private val preferences: AppPreferences? = null
) {

    val allVideos: Flow<List<Video>> = videoDao.getAllVideos().map { list ->
        list.map { it.toDomain() }
    }

    val totalVideoCount: Flow<Int> = videoDao.getTotalVideoCount()

    fun getRecentVideos(limit: Int = 10): Flow<List<Video>> = videoDao.getRecentVideos(limit).map { list ->
        list.map { it.toDomain() }
    }

    fun getVideoById(id: Long): Flow<Video?> = videoDao.getVideoById(id).map { it?.toDomain() }

    fun getVideosForSource(sourceId: Long): Flow<List<Video>> = videoDao.getVideosForSource(sourceId).map { list ->
        list.map { it.toDomain() }
    }

    fun getVideosBySourceType(type: SourceType): Flow<List<Video>> = videoDao.getVideosBySourceType(type.name).map { list ->
        list.map { it.toDomain() }
    }

    fun searchVideos(query: String): Flow<List<Video>> = videoDao.searchVideos(query.trim()).map { list ->
        list.map { it.toDomain() }
    }

    val allSources: Flow<List<Source>> = sourceDao.getAllSources().map { list ->
        list.map { it.toDomain() }
    }

    fun getSourcesByType(type: SourceType): Flow<List<Source>> = sourceDao.getSourcesByType(type.name).map { list ->
        list.map { it.toDomain() }
    }

    fun getSourceById(id: Long): Flow<Source?> = sourceDao.getSourceById(id).map { it?.toDomain() }

    /**
     * Parses and imports a .txt file configuration.
     * Replaces or merges active sources without deleting existing video metadata.
     */
    suspend fun importTxtConfig(content: String, configName: String, replaceAll: Boolean = false): ParsedImportSummary =
        withContext(Dispatchers.IO) {
            val parsed = TxtConfigParser.parse(content)

            if (replaceAll) {
                sourceDao.deleteAllSources()
            }

            for (item in parsed.items) {
                val existing = sourceDao.getSourceByUrl(item.url)
                if (existing == null) {
                    val entity = SourceEntity(
                        type = item.type.name,
                        url = item.url,
                        name = "${item.type.displayName.removeSuffix("s")} ${UrlUtils.extractRemoteId(item.url)}"
                    )
                    sourceDao.insertSource(entity)
                }
            }

            preferences?.recordImport(configName)
            parsed
        }

    /**
     * Synchronizes a single source by running the scraper and deduplicating discovered videos.
     */
    suspend fun syncSource(sourceId: Long): ScrapeResult = withContext(Dispatchers.IO) {
        val entity = sourceDao.getSourceByIdDirect(sourceId)
            ?: return@withContext ScrapeResult.Error("Source not found")

        val normalizedUrl = UrlUtils.normalizeUrl(entity.url)
        val workingEntity = if (normalizedUrl != entity.url) {
            val updated = entity.copy(url = normalizedUrl, updatedAt = System.currentTimeMillis())
            sourceDao.updateSource(updated)
            updated
        } else {
            entity
        }

        val source = workingEntity.toDomain()

        sourceDao.updateSyncStatus(
            id = sourceId,
            status = SyncStatus.SYNCING.name,
            lastChecked = System.currentTimeMillis(),
            lastSuccess = entity.lastSuccessfulSyncAt,
            error = null,
            videoCount = entity.videoCount
        )

        val result = scraperEngine.scrape(source)

        when (result) {
            is ScrapeResult.Success -> {
                var newCount = 0
                var updatedCount = 0

                for (video in result.videos) {
                    // Check if already in DB by pageUrl or remoteId
                    val existing = videoDao.getVideoByPageUrl(video.pageUrl)
                        ?: (video.remoteId?.let { videoDao.getVideoByRemoteId(it) })

                    val videoId: Long = if (existing != null) {
                        // Update with fresh metadata while preserving firstSeenAt
                        val updated = existing.copy(
                            title = if (video.title.isNotBlank()) video.title else existing.title,
                            durationSeconds = video.durationSeconds ?: existing.durationSeconds,
                            uploaderName = video.uploaderName ?: existing.uploaderName,
                            views = video.views ?: existing.views,
                            thumbnailUrl = video.thumbnailUrl ?: existing.thumbnailUrl,
                            playbackReference = video.playbackReference ?: existing.playbackReference,
                            updatedAt = System.currentTimeMillis()
                        )
                        videoDao.updateVideo(updated)
                        updatedCount++
                        existing.id
                    } else {
                        val newEntity = VideoEntity.fromDomain(video)
                        val insertedId = videoDao.insertVideo(newEntity)
                        newCount++
                        insertedId
                    }

                    // Ensure relationship exists in cross ref
                    if (videoId > 0) {
                        sourceDao.insertSource(entity) // ensure safety
                        videoDao.insertCrossRef(SourceVideoCrossRef(sourceId = sourceId, videoId = videoId))
                    }
                }

                val currentCount = videoDao.getVideoCountForSource(sourceId)
                val resolvedName = result.sourceName ?: source.name

                sourceDao.updateSource(
                    entity.copy(
                        name = resolvedName,
                        avatarUrl = entity.avatarUrl ?: result.avatarUrl,
                        syncStatus = SyncStatus.SUCCESS.name,
                        lastCheckedAt = System.currentTimeMillis(),
                        lastSuccessfulSyncAt = System.currentTimeMillis(),
                        lastError = null,
                        videoCount = currentCount,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                ScrapeResult.Success(
                    sourceName = resolvedName,
                    videos = result.videos,
                    newVideosCount = newCount,
                    updatedVideosCount = updatedCount,
                    avatarUrl = result.avatarUrl
                )
            }
            is ScrapeResult.Error -> {
                sourceDao.updateSyncStatus(
                    id = sourceId,
                    status = SyncStatus.ERROR.name,
                    lastChecked = System.currentTimeMillis(),
                    lastSuccess = entity.lastSuccessfulSyncAt,
                    error = result.message,
                    videoCount = entity.videoCount
                )
                result
            }
        }
    }

    suspend fun syncAllEnabledSources(): OverallSyncResult = withContext(Dispatchers.IO) {
        val enabledSources = sourceDao.getEnabledSources()
        var successes = 0
        var failures = 0
        var totalNew = 0
        var totalUpdated = 0

        for (sourceEntity in enabledSources) {
            when (val res = syncSource(sourceEntity.id)) {
                is ScrapeResult.Success -> {
                    successes++
                    totalNew += res.newVideosCount
                    totalUpdated += res.updatedVideosCount
                }
                is ScrapeResult.Error -> {
                    failures++
                }
            }
        }

        OverallSyncResult(
            totalSources = enabledSources.size,
            successfulSources = successes,
            failedSources = failures,
            newVideos = totalNew,
            updatedVideos = totalUpdated
        )
    }

    suspend fun toggleSourceEnabled(sourceId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        val entity = sourceDao.getSourceByIdDirect(sourceId) ?: return@withContext
        sourceDao.updateSource(entity.copy(enabled = enabled, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteSource(sourceId: Long) = withContext(Dispatchers.IO) {
        sourceDao.deleteSourceById(sourceId)
    }

    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        videoDao.deleteAllVideos()
        sourceDao.deleteAllSources()
    }

    suspend fun updateProfileDetails(sourceId: Long, name: String?, customAvatarUri: String?) = withContext(Dispatchers.IO) {
        sourceDao.updateProfileDetails(sourceId, name, customAvatarUri)
    }

    suspend fun getFirstVideoThumbnailForSource(sourceId: Long): String? = withContext(Dispatchers.IO) {
        videoDao.getVideosForSource(sourceId).first().firstOrNull()?.thumbnailUrl
    }
}
