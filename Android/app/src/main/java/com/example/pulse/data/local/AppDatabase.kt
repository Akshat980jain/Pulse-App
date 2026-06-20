package com.example.pulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        PostEntity::class,
        MessageEntity::class,
        DraftEntity::class
    ],
    version = 4,          // Bumped: PostEntity gained commentsDisabled column
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun postDao(): PostDao
    abstract fun messageDao(): MessageDao
    abstract fun draftDao(): DraftDao
}
