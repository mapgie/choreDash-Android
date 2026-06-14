package com.mapgie.dash.widget

import com.mapgie.dash.data.repository.ChoreRepository
import com.mapgie.dash.data.repository.TaskRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Glance widgets and their ActionCallbacks run outside the normal Hilt
 * injection graph (no @AndroidEntryPoint host), so they fetch dependencies
 * via EntryPointAccessors.fromApplication instead.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun taskRepository(): TaskRepository
    fun choreRepository(): ChoreRepository
    fun pinnedItemStore(): PinnedItemStore
}
