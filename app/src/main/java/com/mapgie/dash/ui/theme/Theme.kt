package com.mapgie.dash.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun DashTheme(
    appTheme:   AppTheme = AppTheme.SYSTEM_DEFAULT,
    darkTheme:  Boolean  = isSystemInDarkTheme(),
    customHues: Triple<Float, Float, Float>? = null,
    content:    @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()

    val colorScheme = if (appTheme == AppTheme.CUSTOM && customHues != null) {
        buildCustomColorScheme(
            primaryHue   = customHues.first,
            secondaryHue = customHues.second,
            tertiaryHue  = customHues.third,
            darkTheme    = darkTheme,
        )
    } else {
        colorSchemeFor(appTheme, systemDark)
    }

    val effectivelyDark = when (appTheme) {
        AppTheme.SYSTEM_DEFAULT,
        AppTheme.CORAL_SYSTEM,
        AppTheme.TEAL_SYSTEM -> systemDark
        AppTheme.CUSTOM      -> darkTheme
        else                 -> appTheme.isDark
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !effectivelyDark
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = DashTypography,
        content     = content,
    )
}
