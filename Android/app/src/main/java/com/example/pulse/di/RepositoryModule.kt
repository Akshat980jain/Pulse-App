package com.example.pulse.di

import com.example.pulse.data.repository.SupabaseRepository
import com.example.pulse.data.repository.SupabaseRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindSupabaseRepository(
        impl: SupabaseRepositoryImpl
    ): SupabaseRepository
}
