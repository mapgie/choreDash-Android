package com.mapgie.dash.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// All repositories and infrastructure classes use @Inject constructor and
// @Singleton, so Hilt auto-provides them without explicit @Provides methods.
// This module exists as an extension point for future additions.
@Module
@InstallIn(SingletonComponent::class)
object AppModule
