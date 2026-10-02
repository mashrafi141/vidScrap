package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.SourceVideoCrossRef
import com.example.data.local.entity.VideoEntity
import com.example.data.local.entity.VideoWithSources
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM videos ORDER BY firstSeenAt DESC")
    fun getAllVideos(): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos ORDER BY firstSeenAt DESC LIMIT :limit")
    fun getRecentVideos(limit: Int): Flow<List<VideoEntity>>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    fun getVideoById(id: Long): Flow<VideoEntity?>

    @Query("SELECT * FROM videos WHERE id = :id LIMIT 1")
    suspend fun getVideoByIdDirect(id: Long): VideoEntity?

    @Query("SELECT * FROM videos WHERE pageUrl = :pageUrl LIMIT 1")
    suspend fun getVideoByPageUrl(pageUrl: String): VideoEntity?

    @Query("SELECT * FROM videos WHERE remoteId = :remoteId LIMIT 1")
    suspend fun getVideoByRemoteId(remoteId: String): VideoEntity?

    @Query("""
        SELECT v.* FROM videos v
        INNER JOIN source_video_cross_ref ref ON v.id = ref.videoId
        WHERE ref.sourceId = :sourceId
        ORDER BY v.firstSeenAt DESC
    """)
    fun getVideosForSource(sourceId: Long): Flow<List<VideoEntity>>

    @Query("""
        SELECT DISTINCT v.* FROM videos v
        INNER JOIN source_video_cross_ref ref ON v.id = ref.videoId
        INNER JOIN sources s ON s.id = ref.sourceId
        WHERE s.type = :sourceType
        ORDER BY v.firstSeenAt DESC
    """)
    fun getVideosBySourceType(sourceType: String): Flow<List<VideoEntity>>

    @Query("""
        SELECT * FROM videos 
        WHERE title LIKE '%' || :query || '%' 
           OR uploaderName LIKE '%' || :query || '%'
        ORDER BY firstSeenAt DESC
    """)
    fun searchVideos(query: String): Flow<List<VideoEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideo(video: VideoEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertVideos(videos: List<VideoEntity>): List<Long>

    @Update
    suspend fun updateVideo(video: VideoEntity)

    @Query("DELETE FROM videos WHERE id = :id")
    suspend fun deleteVideoById(id: Long)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: SourceVideoCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRefs(crossRefs: List<SourceVideoCrossRef>)

    @Query("SELECT COUNT(*) FROM videos")
    fun getTotalVideoCount(): Flow<Int>

    @Query("""
        SELECT COUNT(DISTINCT v.id) FROM videos v
        INNER JOIN source_video_cross_ref ref ON v.id = ref.videoId
        WHERE ref.sourceId = :sourceId
    """)
    suspend fun getVideoCountForSource(sourceId: Long): Int

    @Transaction
    @Query("SELECT * FROM videos WHERE id = :id")
    fun getVideoWithSources(id: Long): Flow<VideoWithSources?>
}
