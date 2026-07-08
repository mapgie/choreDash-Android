package com.mapgie.dash.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Message for a widget that failed to load from Supabase, naming how stale the
 * last successfully-loaded data is (if any) rather than a bare "unavailable".
 */
fun unavailableMessage(lastSyncedAt: Instant?): String {
    if (lastSyncedAt == null) return "Can't connect. Open app to check your connection."
    val minutes = ChronoUnit.MINUTES.between(lastSyncedAt, Instant.now())
    return when {
        minutes < 1 -> "Can't refresh right now"
        minutes < 60 -> "Can't refresh. Showing data from ${minutes}m ago"
        else -> "Can't refresh. Showing data from ${minutes / 60}h ago"
    }
}

/** Centred, tappable status text shared by the Next Up and Pinned Item widgets. */
@Composable
fun CenteredMessage(message: String, destination: String) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetActivityIntent(context, destination))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp)
        )
    }
}

/**
 * Fallback for true 1x1 launcher cells: there's no room for a checkbox/button
 * alongside text, so tapping anywhere just opens the app to [destination].
 */
@Composable
fun TinyTapToOpen(label: String, destination: String) {
    val context = LocalContext.current
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(widgetActivityIntent(context, destination))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 2,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        )
    }
}
