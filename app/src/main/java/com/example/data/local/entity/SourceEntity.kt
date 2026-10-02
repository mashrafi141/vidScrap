package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Source
import com.example.core.model.SourceType
import com.example.core.model.SyncStatus

@Entity(
    tableName = "sources",
    indices = [Index(value = ["url"], unique = true)]
)
data class SourceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val url: String,
    val name: String? = null,
    val enabled: Boolean = true,
    val lastCheckedAt: Long? = null,
    val lastSuccessfulSyncAt: Long? = null,
    val lastError: String? = null,
    val videoCount: Int = 0,
    val syncStatus: String = SyncStatus.IDLE.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomain(): Source {
        return Source(
            id = id,
            type = runCatching { SourceType.valueOf(type) }.getOrDefault(SourceType.SINGLE_VIDEO),
            url = url,
            name = name,
            enabled = enabled,
            lastCheckedAt = lastCheckedAt,
            lastSuccessfulSyncAt = lastSuccessfulSyncAt,
            lastError = lastError,
            videoCount = videoCount,
            syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }.getOrDefault(SyncStatus.IDLE),
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDomain(source: Source): SourceEntity {
            return SourceEntity(
                id = source.id,
                type = source.type.name,
                url = source.url,
                name = source.name,
                enabled = source.enabled,
                lastCheckedAt = source.lastCheckedAt,
                lastSuccessfulSyncAt = source.lastSuccessfulSyncAt,
                lastError = source.lastError,
                videoCount = source.videoCount,
                syncStatus = source.syncStatus.name,
                createdAt = source.createdAt,
                updatedAt = source.updatedAt
            )
        }
    }
}
