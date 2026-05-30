package com.mapgie.dash.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// SupabaseClientProvider is @Singleton + @Inject; no manual @Provides needed.
// This module exists as an extension point for mock clients in tests.
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule
