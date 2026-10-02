package com.example.core.model

data class Video(
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
)
