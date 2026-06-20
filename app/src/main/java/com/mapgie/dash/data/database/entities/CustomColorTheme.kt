package com.mapgie.dash.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-saved custom colour theme identified by three HSL hues and a display name.
 * [mode] is one of "LIGHT", "DARK", or "SYSTEM" and records which variant was active
 * when the theme was saved, so it can be restored faithfully.
 */
@Entity(tableName = "custom_color_themes")
data class CustomColorTheme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryHue: Float,
    val secondaryHue: Float,
    val tertiaryHue: Float,
    val mode: String, // "LIGHT", "DARK", or "SYSTEM"
)
