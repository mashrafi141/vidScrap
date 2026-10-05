package com.example.core.model

data class Source(
    val id: Long = 0,
    val type: SourceType,
    val url: String,
    val name: String? = null,
    val enabled: Boolean = true,
    val lastCheckedAt: Long? = null,
    val lastSuccessfulSyncAt: Long? = null,
    val lastError: String? = null,
    val videoCount: Int = 0,
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val avatarUrl: String? = null,
    val customAvatarUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
