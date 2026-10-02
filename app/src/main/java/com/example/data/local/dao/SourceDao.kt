package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SourceEntity
import com.example.data.local.entity.SourceWithVideos
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources ORDER BY createdAt DESC")
    fun getAllSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE type = :type ORDER BY createdAt DESC")
    fun getSourcesByType(type: String): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources WHERE enabled = 1")
    suspend fun getEnabledSources(): List<SourceEntity>

    @Query("SELECT * FROM sources WHERE id = :id LIMIT 1")
    fun getSourceById(id: Long): Flow<SourceEntity?>

    @Query("SELECT * FROM sources WHERE id = :id LIMIT 1")
    suspend fun getSourceByIdDirect(id: Long): SourceEntity?

    @Query("SELECT * FROM sources WHERE url = :url LIMIT 1")
    suspend fun getSourceByUrl(url: String): SourceEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSource(source: SourceEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSources(sources: List<SourceEntity>): List<Long>

    @Update
    suspend fun updateSource(source: SourceEntity)

    @Delete
    suspend fun deleteSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE id = :id")
    suspend fun deleteSourceById(id: Long)

    @Query("DELETE FROM sources")
    suspend fun deleteAllSources()

    @Query("""
        UPDATE sources 
        SET syncStatus = :status, 
            lastCheckedAt = :lastChecked, 
            lastSuccessfulSyncAt = COALESCE(:lastSuccess, lastSuccessfulSyncAt), 
            lastError = :error, 
            videoCount = :videoCount,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateSyncStatus(
        id: Long,
        status: String,
        lastChecked: Long?,
        lastSuccess: Long?,
        error: String?,
        videoCount: Int,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Transaction
    @Query("SELECT * FROM sources WHERE id = :id")
    fun getSourceWithVideos(id: Long): Flow<SourceWithVideos?>
}
