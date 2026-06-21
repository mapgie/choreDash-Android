package com.mapgie.dash.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user-saved custom colour theme identified by three sets of HSL values and a display name.
 * [mode] is one of "LIGHT", "DARK", or "SYSTEM" and records which variant was active
 * when the theme was saved, so it can be restored faithfully.
 *
 * Columns added in version 2:
 * - primarySaturation, primaryLightness (0..1)
 * - secondarySaturation, secondaryLightness (0..1)
 * - tertiarySaturation, tertiaryLightness (0..1)
 */
@Entity(tableName = "custom_color_themes")
data class CustomColorTheme(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryHue: Float,
    val secondaryHue: Float,
    val tertiaryHue: Float,
    val mode: String, // "LIGHT", "DARK", or "SYSTEM"
    val primarySaturation: Float = 0.5f,
    val primaryLightness: Float = 0.4f,
    val secondarySaturation: Float = 0.4f,
    val secondaryLightness: Float = 0.4f,
    val tertiarySaturation: Float = 0.4f,
    val tertiaryLightness: Float = 0.4f,
)
