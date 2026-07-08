package com.mapgie.dash.widget

import androidx.glance.material3.ColorProviders
import com.mapgie.dash.ui.theme.MistDarkColors
import com.mapgie.dash.ui.theme.MistLightColors

/**
 * Mirrors DashTheme's default Mist palette for Glance widgets, which
 * cannot read MaterialTheme/CompositionLocal values from the host app.
 */
object DashGlanceTheme {
    val colors = ColorProviders(
        light = MistLightColors,
        dark = MistDarkColors
    )
}
