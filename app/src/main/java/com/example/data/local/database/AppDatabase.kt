package com.example.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.data.local.dao.SourceDao
import com.example.data.local.dao.VideoDao
import com.example.data.local.entity.SourceEntity
import com.example.data.local.entity.SourceVideoCrossRef
import com.example.data.local.entity.VideoEntity

@Database(
    entities = [
        SourceEntity::class,
        VideoEntity::class,
        SourceVideoCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao
    abstract fun videoDao(): VideoDao
}
