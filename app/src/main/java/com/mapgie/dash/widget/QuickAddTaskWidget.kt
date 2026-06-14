package com.mapgie.dash.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.GlanceTheme
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle

/** Small widget: tapping it opens choreDash straight to the "Add task" sheet. */
class QuickAddTaskWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(WIDGET_RESPONSIVE_SIZES)

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme(colors = DashGlanceTheme.colors) {
                QuickAddTaskContent()
            }
        }
    }
}

class QuickAddTaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddTaskWidget()
}

@Composable
private fun QuickAddTaskContent() {
    val context = LocalContext.current
    val showLabel = LocalSize.current.width >= WIDGET_SIZE_MEDIUM.width

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.primary)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(widgetActivityIntent(context, WIDGET_DEST_QUICK_ADD_TASK)))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+",
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Add task",
                    style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 14.sp)
                )
            }
        } else {
            Text(
                text = "+",
                style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
