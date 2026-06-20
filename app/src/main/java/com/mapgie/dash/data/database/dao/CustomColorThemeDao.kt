package com.mapgie.dash.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.mapgie.dash.data.database.entities.CustomColorTheme
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomColorThemeDao {

    /** Live-updating list of all saved themes, ordered by name. */
    @Query("SELECT * FROM custom_color_themes ORDER BY name ASC")
    fun getAll(): Flow<List<CustomColorTheme>>

    /**
     * Insert or update a theme. Returns the row ID of the upserted row,
     * which can be used to track the active profile.
     */
    @Upsert
    suspend fun upsert(theme: CustomColorTheme): Long

    /** Delete a saved theme. */
    @Delete
    suspend fun delete(theme: CustomColorTheme)

    /** Look up a single theme by ID (returns null if not found). */
    @Query("SELECT * FROM custom_color_themes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): CustomColorTheme?
}
