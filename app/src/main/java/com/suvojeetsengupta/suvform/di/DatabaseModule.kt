package com.suvojeetsengupta.suvform.di

import android.content.Context
import androidx.room.Room
import com.suvojeetsengupta.suvform.data.local.FormDao
import com.suvojeetsengupta.suvform.data.local.SuvFormDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SuvFormDatabase =
        Room.databaseBuilder(context, SuvFormDatabase::class.java, "suvform.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideFormDao(db: SuvFormDatabase): FormDao = db.formDao()

    @Provides
    fun provideResponseDao(db: SuvFormDatabase): ResponseDao = db.responseDao()
}
