package com.example.pulse.di

import android.content.Context
import androidx.room.Room
import com.example.pulse.data.local.AppDatabase
import com.example.pulse.data.local.DraftDao
import com.example.pulse.data.local.MessageDao
import com.example.pulse.data.local.PostDao
import com.example.pulse.data.local.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pulse_cache_db"
        )
        .fallbackToDestructiveMigration() // Destructive migration for ease of local prototyping
        .build()
    }

    @Provides
    fun provideProfileDao(db: AppDatabase): ProfileDao = db.profileDao()

    @Provides
    fun providePostDao(db: AppDatabase): PostDao = db.postDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideDraftDao(db: AppDatabase): DraftDao = db.draftDao()
}
