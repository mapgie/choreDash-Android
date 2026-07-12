package com.mapgie.dash.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
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
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.mapgie.dash.R
import com.mapgie.dash.ui.theme.TypeTaskContainer
import com.mapgie.dash.ui.theme.TypeTaskOnContainer

/** 1x1 widget: tapping it opens choreDash straight to the "Add task" sheet. */
class QuickAddTaskWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(WIDGET_QUICK_ADD_SIZES)

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
    val onContainer = ColorProvider(TypeTaskOnContainer)

    // A rounded square (as opposed to the Add Chore widget's pill/circle) plus the
    // app's fixed Task accent colour (same one used on the bottom nav and add-menu
    // FAB, stable across all colour themes), so the two quick-add widgets stay
    // distinguishable by shape and colour, not just their icon.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(TypeTaskContainer))
            .cornerRadius(14.dp)
            .clickable(actionStartActivity(widgetActivityIntent(context, WIDGET_DEST_QUICK_ADD_TASK)))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_task),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(onContainer),
                    modifier = GlanceModifier.size(28.dp)
                )
                Text(
                    text = "Add task",
                    style = TextStyle(color = onContainer, fontSize = 14.sp)
                )
            }
        } else {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_task),
                contentDescription = "Add task",
                colorFilter = ColorFilter.tint(onContainer),
                modifier = GlanceModifier.size(28.dp)
            )
        }
    }
}
