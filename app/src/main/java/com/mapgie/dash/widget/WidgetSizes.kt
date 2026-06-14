package com.mapgie.dash.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// Responsive breakpoints shared by all choreDash widgets, roughly matching
// 2x1/2x2, 4x2 and 4x4 launcher grid cells.
val WIDGET_SIZE_SMALL = DpSize(110.dp, 110.dp)
val WIDGET_SIZE_MEDIUM = DpSize(250.dp, 110.dp)
val WIDGET_SIZE_LARGE = DpSize(250.dp, 250.dp)

val WIDGET_RESPONSIVE_SIZES = setOf(WIDGET_SIZE_SMALL, WIDGET_SIZE_MEDIUM, WIDGET_SIZE_LARGE)
