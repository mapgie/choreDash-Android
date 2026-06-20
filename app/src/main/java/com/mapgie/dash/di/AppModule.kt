package com.mapgie.dash.di

import android.content.Context
import com.mapgie.dash.data.database.AppDatabase
import com.mapgie.dash.data.database.dao.CustomColorThemeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideCustomColorThemeDao(db: AppDatabase): CustomColorThemeDao =
        db.customColorThemeDao()
}
