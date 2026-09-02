package com.mapgie.dash.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.CategoryIcon

/**
 * Lucide stroke icons (2dp strokes, round caps and joins, 24x24 viewport) used
 * everywhere the Zen Dark / Cozy Cream handoff calls for them: the tab bar, the
 * header action row, per-category card chips and the sheet controls.
 *
 * Path data is taken verbatim from the handoff's inline SVGs (which are Lucide
 * glyphs) and parsed at first use. Everything is drawn in black so
 * `Icon(tint = ...)` recolours it like any Material icon.
 *
 * Dots in the source SVGs are `h.01` micro-strokes; they are widened to `h.2`
 * so the round caps reliably render as points on every device.
 */
object LucideIcons {

    private const val VIEWPORT = 24f

    /** SVG `<circle cx cy r>` as a path: two half-circle arcs. */
    private fun circle(cx: Float, cy: Float, r: Float): String =
        "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

    /** SVG `<rect x y width height rx>` as a path. */
    private fun rect(x: Float, y: Float, w: Float, h: Float, rx: Float): String =
        "M${x + rx} ${y}h${w - 2 * rx}a$rx $rx 0 0 1 $rx ${rx}v${h - 2 * rx}a$rx $rx 0 0 1 ${-rx} ${rx}h${-(w - 2 * rx)}a$rx $rx 0 0 1 ${-rx} ${-rx}v${-(h - 2 * rx)}a$rx $rx 0 0 1 $rx ${-rx}z"

    private fun lucide(
        name: String,
        strokes: List<String>,
        fills: List<String> = emptyList(),
        strokeWidth: Float = 2f,
    ): ImageVector = ImageVector.Builder(
        name = "Lucide.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = VIEWPORT,
        viewportHeight = VIEWPORT,
    ).apply {
        strokes.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach { d ->
            addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = SolidColor(Color.Black),
                stroke = null,
            )
        }
    }.build()

    // ── Category glyphs ──────────────────────────────────────────────────────

    val Droplet: ImageVector by lazy {
        lucide("Droplet", listOf("M12 3c3 4 6 7.5 6 11a6 6 0 0 1-12 0c0-3.5 3-7 6-11z"))
    }

    val Home: ImageVector by lazy {
        lucide("Home", listOf("M3 11 12 4l9 7", "M5 10v10h14V10"))
    }

    /** House outline with a check inside: the Chores tab glyph. */
    val HouseCheck: ImageVector by lazy {
        lucide("HouseCheck", listOf("M3 11 12 4l9 7", "M5 10v10h14V10", "m9 15 2 2 4-4"))
    }

    val Car: ImageVector by lazy {
        lucide(
            "Car",
            listOf(
                "M5 13 6.5 8.5A2 2 0 0 1 8.4 7h7.2a2 2 0 0 1 1.9 1.5L19 13",
                rect(3f, 13f, 18f, 5f, 1.5f),
                "M6 18v2M18 18v2",
            ),
        )
    }

    val WashingMachine: ImageVector by lazy {
        lucide(
            "WashingMachine",
            listOf(rect(4f, 3f, 16f, 18f, 2f), circle(12f, 13f, 4f), "M7 6.5h.2M10 6.5h.2"),
        )
    }

    val Sprout: ImageVector by lazy {
        lucide(
            "Sprout",
            listOf("M12 21v-8", "M12 13c-4 0-7-3-7-7 4 0 7 3 7 7z", "M12 13c4 0 7-3 7-7-4 0-7 3-7 7z"),
        )
    }

    val Utensils: ImageVector by lazy {
        lucide(
            "Utensils",
            listOf(
                "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2",
                "M7 2v20",
                "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7",
            ),
        )
    }

    val Bath: ImageVector by lazy {
        lucide(
            "Bath",
            listOf("M9 6 6.5 3.5a1.5 1.5 0 0 0-1-.5C4.7 3 4 3.7 4 4.5V17a5 5 0 0 0 5 5h6a5 5 0 0 0 5-5v-4", "M4 12h16"),
        )
    }

    val TreePine: ImageVector by lazy {
        lucide(
            "TreePine",
            listOf(
                "m17 14 3 3.3a1 1 0 0 1-.7 1.7H4.7a1 1 0 0 1-.7-1.7L7 14h-.3a1 1 0 0 1-.7-1.7L9 9h-.2A1 1 0 0 1 8 7.3L12 3l4 4.3a1 1 0 0 1-.8 1.7H15l3 3.3a1 1 0 0 1-.7 1.7H17Z",
                "M12 22v-3",
            ),
        )
    }

    val Zap: ImageVector by lazy {
        lucide("Zap", listOf("M13 2 3 14h7l-1 8 10-12h-7l1-8z"))
    }

    val Pill: ImageVector by lazy {
        lucide("Pill", listOf("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7z", "m8.5 8.5 7 7"))
    }

    val Printer: ImageVector by lazy {
        lucide(
            "Printer",
            listOf(
                "M6 9V2h12v7",
                "M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2",
                "M6 14h12v8H6z",
            ),
        )
    }

    val Plane: ImageVector by lazy {
        lucide(
            "Plane",
            listOf("M17.8 19.2 16 11l3.5-3.5C21 6 21.5 4 21 3c-1-.5-3 0-4.5 1.5L13 8 4.8 6.2c-.5-.1-.9.1-1.1.5l-.3.5c-.2.5-.1 1 .3 1.3L9 12l-2 3H4l-1 1 3 2 2 3 1-1v-3l3-2 3.5 5.3c.3.4.8.5 1.3.3l.5-.2c.4-.3.6-.7.5-1.2z"),
        )
    }

    val Shield: ImageVector by lazy {
        lucide("Shield", listOf("M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"))
    }

    val Calendar: ImageVector by lazy {
        lucide("Calendar", listOf(rect(3f, 4f, 18f, 18f, 3f), "M8 2v4M16 2v4M3 10h18"))
    }

    val Brush: ImageVector by lazy {
        lucide("Brush", listOf("M20.4 14.5 16 10 4 20", "M12 6 8 2 4 6l4 4", "m14 4 6 6"))
    }

    val Leaf: ImageVector by lazy {
        lucide(
            "Leaf",
            listOf(
                "M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.48 19 2c1 2 2 4.18 2 8 0 5.5-4.78 10-10 10Z",
                "M2 21c0-3 1.85-5.36 5.08-6C9.5 14.52 12 13 13 12",
            ),
        )
    }

    val Lamp: ImageVector by lazy {
        lucide("Lamp", listOf("M8 2h8l4 10H4L8 2Z", "M12 12v6", "M8 22v-2c0-1.1.9-2 2-2h4a2 2 0 0 1 2 2v2H8Z"))
    }

    val CircleAlert: ImageVector by lazy {
        lucide("CircleAlert", listOf(circle(12f, 12f, 9f), "M12 8v4M12 16h.2"))
    }

    // ── Navigation and header ────────────────────────────────────────────────

    val CircleCheck: ImageVector by lazy {
        lucide("CircleCheck", listOf("M22 11.08V12a10 10 0 1 1-5.93-9.14", "m9 11 3 3L22 4"))
    }

    val Bell: ImageVector by lazy {
        lucide("Bell", listOf("M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9", "M10.3 21a1.94 1.94 0 0 0 3.4 0"))
    }

    val Settings: ImageVector by lazy {
        lucide(
            "Settings",
            listOf(
                circle(12f, 12f, 3f),
                "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4",
            ),
        )
    }

    val Search: ImageVector by lazy {
        lucide("Search", listOf(circle(11f, 11f, 7f), "m21 21-4-4"), strokeWidth = 1.8f)
    }

    val User: ImageVector by lazy {
        lucide("User", listOf(circle(12f, 8f, 4f), "M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"), strokeWidth = 1.8f)
    }

    /** Filled person: the "just mine" owner scope, distinguishable from [User] by shape. */
    val UserFilled: ImageVector by lazy {
        lucide(
            "UserFilled",
            strokes = listOf(circle(12f, 8f, 4f), "M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"),
            fills = listOf(circle(12f, 8f, 4f), "M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5z"),
            strokeWidth = 1.8f,
        )
    }

    /** Two people: the "everyone" owner scope. */
    val Users: ImageVector by lazy {
        lucide(
            "Users",
            listOf(
                "M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2",
                circle(9f, 7f, 4f),
                "M22 21v-2a4 4 0 0 0-3-3.87",
                "M16 3.13a4 4 0 0 1 0 7.75",
            ),
            strokeWidth = 1.8f,
        )
    }

    /** Three concentric circles: the zen glyph. */
    val Target: ImageVector by lazy {
        lucide("Target", listOf(circle(12f, 12f, 10f), circle(12f, 12f, 6f), circle(12f, 12f, 2f)), strokeWidth = 1.8f)
    }

    val LayoutGrid: ImageVector by lazy {
        lucide(
            "LayoutGrid",
            listOf(rect(3f, 3f, 7f, 7f, 1.5f), rect(14f, 3f, 7f, 7f, 1.5f), rect(3f, 14f, 7f, 7f, 1.5f), rect(14f, 14f, 7f, 7f, 1.5f)),
            strokeWidth = 1.8f,
        )
    }

    val List: ImageVector by lazy {
        lucide("List", listOf("M8 6h13M8 12h13M8 18h13M3 6h.2M3 12h.2M3 18h.2"), strokeWidth = 1.8f)
    }

    /** Two arcs over a dot: the NFC glyph on cards and sheets. */
    val Nfc: ImageVector by lazy {
        lucide(
            "Nfc",
            strokes = listOf("M6 8.5a8 8 0 0 1 12 0", "M8.5 11.5a4.5 4.5 0 0 1 7 0"),
            fills = listOf(circle(12f, 15f, 1.2f)),
        )
    }

    /** [Nfc] inside four corner brackets: the header "scan a tag" button. */
    val NfcScan: ImageVector by lazy {
        lucide(
            "NfcScan",
            strokes = listOf(
                "M6 8.5a8 8 0 0 1 12 0",
                "M8.5 11.5a4.5 4.5 0 0 1 7 0",
                "M4 18v2a1 1 0 0 0 1 1h2M17 21h2a1 1 0 0 0 1-1v-2M4 6V4a1 1 0 0 1 1-1h2M17 3h2a1 1 0 0 1 1 1v2",
            ),
            fills = listOf(circle(12f, 15f, 1.2f)),
            strokeWidth = 1.9f,
        )
    }

    // ── Controls ─────────────────────────────────────────────────────────────

    val Plus: ImageVector by lazy { lucide("Plus", listOf("M12 5v14M5 12h14"), strokeWidth = 2.4f) }
    val X: ImageVector by lazy { lucide("X", listOf("M6 6l12 12M18 6 6 18"), strokeWidth = 2.4f) }
    val Check: ImageVector by lazy { lucide("Check", listOf("m5 12 5 5L20 7"), strokeWidth = 2.6f) }
    val Minus: ImageVector by lazy { lucide("Minus", listOf("M5 12h14"), strokeWidth = 2.4f) }
    val ChevronDown: ImageVector by lazy { lucide("ChevronDown", listOf("m6 9 6 6 6-6"), strokeWidth = 2.4f) }
    val ChevronUp: ImageVector by lazy { lucide("ChevronUp", listOf("m18 15-6-6-6 6"), strokeWidth = 2.4f) }
    val ChevronRight: ImageVector by lazy { lucide("ChevronRight", listOf("m9 6 6 6-6 6"), strokeWidth = 2.2f) }
    val ChevronLeft: ImageVector by lazy { lucide("ChevronLeft", listOf("m15 18-6-6 6-6")) }

    val Share: ImageVector by lazy {
        lucide("Share", listOf("M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", "m16 6-4-4-4 4M12 2v13"))
    }

    val Archive: ImageVector by lazy {
        lucide("Archive", listOf(rect(3f, 4f, 18f, 4f, 1f), "M5 8v12h14V8M10 12h4"))
    }

    val Trash: ImageVector by lazy {
        lucide("Trash", listOf("M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"))
    }

    val Pencil: ImageVector by lazy {
        lucide("Pencil", listOf("M12 20h9", "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"))
    }

    val Pin: ImageVector by lazy {
        lucide("Pin", listOf("M12 17v5M9 3h6l-1 7 3 3H7l3-3z"))
    }

    val PinFilled: ImageVector by lazy {
        lucide("PinFilled", strokes = listOf("M12 17v5M9 3h6l-1 7 3 3H7l3-3z"), fills = listOf("M9 3h6l-1 7 3 3H7l3-3z"))
    }

    val Undo: ImageVector by lazy {
        lucide("Undo", listOf("M3 7v6h6", "M21 17a9 9 0 0 0-15-6.7L3 13"), strokeWidth = 2.4f)
    }

    val Repeat: ImageVector by lazy {
        lucide("Repeat", listOf("M21 12a9 9 0 1 1-3-6.7", "M21 3v5h-5"))
    }

    val GripVertical: ImageVector by lazy {
        lucide("GripVertical", listOf("M9 6h.2M15 6h.2M9 12h.2M15 12h.2M9 18h.2M15 18h.2"))
    }

    val ArrowUp: ImageVector by lazy { lucide("ArrowUp", listOf("M12 19V5", "m5 12 7-7 7 7")) }
    val Play: ImageVector by lazy { lucide("Play", listOf("M6 3 20 12 6 21z")) }
    val CircleHelp: ImageVector by lazy {
        lucide("CircleHelp", listOf(circle(12f, 12f, 9f), "M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3", "M12 17h.2"))
    }
    val ArrowDown: ImageVector by lazy { lucide("ArrowDown", listOf("M12 5v14", "m19 12-7 7-7-7")) }

    /** Bell with a slash: an alarm that is muted or snoozed. */
    val BellOff: ImageVector by lazy {
        lucide(
            "BellOff",
            listOf(
                "M8.7 3A6 6 0 0 1 18 8a21.3 21.3 0 0 0 .6 5",
                "M17 17H3s3-2 3-9a4.67 4.67 0 0 1 .3-1.7",
                "M10.3 21a1.94 1.94 0 0 0 3.4 0",
                "m2 2 20 20",
            ),
        )
    }

    /** Resolves the drawable for a persisted [CategoryIcon]. */
    fun forCategory(icon: CategoryIcon): ImageVector = when (icon) {
        CategoryIcon.DROPLET -> Droplet
        CategoryIcon.HOME -> Home
        CategoryIcon.CAR -> Car
        CategoryIcon.WASHING_MACHINE -> WashingMachine
        CategoryIcon.SPROUT -> Sprout
        CategoryIcon.UTENSILS -> Utensils
        CategoryIcon.BATH -> Bath
        CategoryIcon.TREE_PINE -> TreePine
        CategoryIcon.ZAP -> Zap
        CategoryIcon.PILL -> Pill
        CategoryIcon.PRINTER -> Printer
        CategoryIcon.PLANE -> Plane
        CategoryIcon.SHIELD -> Shield
        CategoryIcon.CALENDAR -> Calendar
        CategoryIcon.BRUSH -> Brush
        CategoryIcon.LEAF -> Leaf
        CategoryIcon.LAMP -> Lamp
        CategoryIcon.CIRCLE_ALERT -> CircleAlert
    }
}
