package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Video

@Entity(
    tableName = "videos",
    indices = [
        Index(value = ["pageUrl"], unique = true),
        Index(value = ["remoteId"]),
        Index(value = ["firstSeenAt"])
    ]
)
data class VideoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val remoteId: String? = null,
    val sourceUrl: String,
    val pageUrl: String,
    val title: String,
    val durationSeconds: Long? = null,
    val uploaderName: String? = null,
    val uploaderUrl: String? = null,
    val views: Long? = null,
    val publishedAt: String? = null,
    val thumbnailUrl: String? = null,
    val thumbnailCacheKey: String? = null,
    val playbackReference: String? = null,
    val firstSeenAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Video {
        return Video(
            id = id,
            remoteId = remoteId,
            sourceUrl = sourceUrl,
            pageUrl = pageUrl,
            title = title,
            durationSeconds = durationSeconds,
            uploaderName = uploaderName,
            uploaderUrl = uploaderUrl,
            views = views,
            publishedAt = publishedAt,
            thumbnailUrl = thumbnailUrl,
            thumbnailCacheKey = thumbnailCacheKey,
            playbackReference = playbackReference,
            firstSeenAt = firstSeenAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(video: Video): VideoEntity {
            return VideoEntity(
                id = video.id,
                remoteId = video.remoteId,
                sourceUrl = video.sourceUrl,
                pageUrl = video.pageUrl,
                title = video.title,
                durationSeconds = video.durationSeconds,
                uploaderName = video.uploaderName,
                uploaderUrl = video.uploaderUrl,
                views = video.views,
                publishedAt = video.publishedAt,
                thumbnailUrl = video.thumbnailUrl,
                thumbnailCacheKey = video.thumbnailCacheKey,
                playbackReference = video.playbackReference,
                firstSeenAt = video.firstSeenAt,
                updatedAt = video.updatedAt
            )
        }
    }
}
