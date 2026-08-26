package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Cozy Cream page header for the four main tabs: a 5dp horizontal gradient strip
 * across the very top of the screen, then the page title set lowercase in the
 * serif headline face with a full stop tinted in the tab's accent colour
 * ("tasks." / "chores." / "settings.").
 *
 * The gradient runs secondary → tertiaryContainer → tertiary so it stays in
 * palette for every theme (on Cream that is exactly the design's
 * sage → cream-gold → amber strip).
 *
 * [actions] slots trailing controls (filter/zen/sort icon buttons) on the title
 * row, baseline-centred against the title.
 */
@Composable
fun PageHeader(
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.tertiary,
                        )
                    )
                )
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 14.dp, bottom = 2.dp)
        ) {
            Text(
                text = buildAnnotatedString {
                    append(title.lowercase())
                    withStyle(SpanStyle(color = accent)) { append(".") }
                },
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}
