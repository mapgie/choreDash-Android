package com.mapgie.dash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Carries all nine HSL values for the custom colour theme so [DashTheme] can
 * forward them to [buildCustomColorScheme] in one parameter.
 */
data class CustomHSL(
    val primaryH: Float,   val primaryS: Float,   val primaryL: Float,
    val secondaryH: Float, val secondaryS: Float, val secondaryL: Float,
    val tertiaryH: Float,  val tertiaryS: Float,  val tertiaryL: Float,
)

@Composable
fun DashTheme(
    appTheme:  AppTheme = AppTheme.MIST,
    darkTheme: Boolean  = isSystemInDarkTheme(),
    customHSL: CustomHSL? = null,
    content:   @Composable () -> Unit,
) {
    val colorScheme = if (appTheme == AppTheme.CUSTOM && customHSL != null) {
        buildCustomColorScheme(
            primaryH   = customHSL.primaryH,
            primaryS   = customHSL.primaryS,
            primaryL   = customHSL.primaryL,
            secondaryH = customHSL.secondaryH,
            secondaryS = customHSL.secondaryS,
            secondaryL = customHSL.secondaryL,
            tertiaryH  = customHSL.tertiaryH,
            tertiaryS  = customHSL.tertiaryS,
            tertiaryL  = customHSL.tertiaryL,
            darkTheme  = darkTheme,
        )
    } else {
        colorSchemeFor(appTheme, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = DashTypography,
        content     = content,
    )
}
