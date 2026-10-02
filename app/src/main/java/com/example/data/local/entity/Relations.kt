package com.example.data.local.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SourceWithVideos(
    @Embedded val source: SourceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SourceVideoCrossRef::class,
            parentColumn = "sourceId",
            entityColumn = "videoId"
        )
    )
    val videos: List<VideoEntity>
)

data class VideoWithSources(
    @Embedded val video: VideoEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SourceVideoCrossRef::class,
            parentColumn = "videoId",
            entityColumn = "sourceId"
        )
    )
    val sources: List<SourceEntity>
)
