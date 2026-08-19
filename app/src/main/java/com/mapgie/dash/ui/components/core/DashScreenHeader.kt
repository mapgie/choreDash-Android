package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.Dimens

/**
 * The thin identity strip at the top of every list tab. It names the page in the
 * tab's own type-accent colour, matching the lit tab in the bottom nav, so the strip
 * at the top and the tab at the bottom answer "where am I" together.
 *
 * Administrative, not a display heading: a single letter-spaced [labelMedium] line
 * roughly 28dp tall, so it names the page without competing with the list. Colours
 * come from `LocalTypeAccents` (passed in by the screen), the same pair driving the
 * nav pill, so it stays fixed and legible in dark mode.
 */
@Composable
fun DashScreenHeader(
    title: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = Dimens.cardInset, vertical = 6.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.5.sp),
            color = contentColor,
        )
    }
}
