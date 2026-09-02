package com.mapgie.dash.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Cozy Cream glyphs that have no Material icon equivalent, traced from the
 * design's inline SVGs on a 24x24 viewport. All are stroke-only (no fill) and
 * drawn in black so `Icon(tint = ...)` recolours them like any Material icon.
 */
object DashIcons {

    /** Scrub brush: rounded rectangular head with four bristle strokes. The chores glyph. */
    val Brush: ImageVector by lazy {
        ImageVector.Builder(
            name = "DashIcons.Brush",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Head: rect x=4 y=5 w=16 h=5 rx=2.5
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 5f)
                lineTo(17.5f, 5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 20f, 7.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 17.5f, 10f)
                lineTo(6.5f, 10f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4f, 7.5f)
                arcTo(2.5f, 2.5f, 0f, isMoreThanHalf = false, isPositiveArc = true, 6.5f, 5f)
                close()
            }
            // Bristles
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7f, 10f); verticalLineTo(15f)
                moveTo(10.5f, 10f); verticalLineTo(16.5f)
                moveTo(13.5f, 10f); verticalLineTo(16.5f)
                moveTo(17f, 10f); verticalLineTo(15f)
            }
        }.build()
    }

    /** Three concentric circles (radii 10, 6, 2). The zen-mode glyph. */
    val Zen: ImageVector by lazy {
        ImageVector.Builder(
            name = "DashIcons.Zen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                for (r in listOf(10f, 6f, 2f)) {
                    moveTo(12f - r, 12f)
                    arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f + r, 12f)
                    arcTo(r, r, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f - r, 12f)
                    close()
                }
            }
        }.build()
    }

    /** Four corner brackets around three dots: the "hold phone to tag" NFC glyph. */
    val NfcTap: ImageVector by lazy {
        ImageVector.Builder(
            name = "DashIcons.NfcTap",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            // Corner brackets
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // M4 7V5a1 1 0 0 1 1-1h2
                moveTo(4f, 7f)
                verticalLineTo(5f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, -1f)
                horizontalLineToRelative(2f)
                // M17 4h2a1 1 0 0 1 1 1v2
                moveTo(17f, 4f)
                horizontalLineToRelative(2f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, 1f, 1f)
                verticalLineToRelative(2f)
                // M20 17v2a1 1 0 0 1-1 1h-2
                moveTo(20f, 17f)
                verticalLineToRelative(2f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1f, 1f)
                horizontalLineToRelative(-2f)
                // M7 20H5a1 1 0 0 1-1-1v-2
                moveTo(7f, 20f)
                horizontalLineTo(5f)
                arcToRelative(1f, 1f, 0f, isMoreThanHalf = false, isPositiveArc = true, -1f, -1f)
                verticalLineToRelative(-2f)
            }
            // Dots: very short strokes so the round caps render as points on every renderer
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 12f); horizontalLineToRelative(0.2f)
                moveTo(12f, 12f); horizontalLineToRelative(0.2f)
                moveTo(16f, 12f); horizontalLineToRelative(0.2f)
            }
        }.build()
    }
}
