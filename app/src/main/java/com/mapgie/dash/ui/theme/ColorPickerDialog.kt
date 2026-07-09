package com.mapgie.dash.ui.theme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/**
 * Full colour picker dialog: an HSV saturation/brightness square, a hue bar,
 * and a hex field, all kept in sync. Confirms with the composed ARGB.
 *
 * This is the one picker used for every custom colour role so the picking
 * experience stays consistent across the app (and across apps built from
 * this template).
 */
@Composable
fun ColorPickerDialog(
    label:       String,
    currentArgb: Int,
    onDismiss:   () -> Unit,
    onConfirm:   (Int) -> Unit,
) {
    val startHsv = FloatArray(3).also { AndroidColor.colorToHSV(currentArgb, it) }

    var hue        by remember { mutableStateOf(startHsv[0]) }
    var saturation by remember { mutableStateOf(startHsv[1]) }
    var hsvalue    by remember { mutableStateOf(startHsv[2]) }
    var hexText    by remember { mutableStateOf("%06X".format(currentArgb and 0xFFFFFF)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label) },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, hsvalue))))
                )
                HsvSquarePicker(
                    hue        = hue,
                    saturation = saturation,
                    value      = hsvalue,
                    onSaturationValueChange = { s, v ->
                        saturation = s
                        hsvalue    = v
                        hexText = "%06X".format(AndroidColor.HSVToColor(floatArrayOf(hue, s, v)) and 0xFFFFFF)
                    },
                )
                HuePicker(
                    hue         = hue,
                    onHueChange = { h ->
                        hue     = h
                        hexText = "%06X".format(AndroidColor.HSVToColor(floatArrayOf(h, saturation, hsvalue)) and 0xFFFFFF)
                    },
                )
                OutlinedTextField(
                    value         = hexText,
                    onValueChange = { input ->
                        val clean = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                        hexText = clean
                        if (clean.length == 6) {
                            clean.toLongOrNull(16)?.let { rgb ->
                                val argb = (0xFF000000L or rgb).toInt()
                                val hsv  = FloatArray(3)
                                AndroidColor.colorToHSV(argb, hsv)
                                hue = hsv[0]; saturation = hsv[1]; hsvalue = hsv[2]
                            }
                        }
                    },
                    label           = { Text("HEX") },
                    prefix          = { Text("#") },
                    singleLine      = true,
                    isError         = hexText.isNotEmpty() && hexText.length < 6,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier        = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, hsvalue)))
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// ── HSV saturation/brightness square ──────────────────────────────────────────

@Composable
private fun HsvSquarePicker(
    hue:                     Float,
    saturation:              Float,
    value:                   Float,
    onSaturationValueChange: (Float, Float) -> Unit,
    modifier:                Modifier = Modifier,
) {
    val pureHueColor = Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(8.dp))
            .semantics { contentDescription = "Colour saturation and brightness selector" }
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val w = size.width.toFloat().coerceAtLeast(1f)
                    val h = size.height.toFloat().coerceAtLeast(1f)
                    onSaturationValueChange(
                        (down.position.x / w).coerceIn(0f, 1f),
                        1f - (down.position.y / h).coerceIn(0f, 1f),
                    )
                    drag(down.id) { change ->
                        onSaturationValueChange(
                            (change.position.x / w).coerceIn(0f, 1f),
                            1f - (change.position.y / h).coerceIn(0f, 1f),
                        )
                        change.consume()
                    }
                }
            }
    ) {
        drawRect(brush = Brush.horizontalGradient(listOf(Color.White, pureHueColor)))
        drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        val tx = saturation * size.width
        val ty = (1f - value) * size.height
        drawCircle(Color.Black.copy(alpha = 0.3f), 12.dp.toPx(), Offset(tx, ty))
        drawCircle(Color.White,                    11.dp.toPx(), Offset(tx, ty), style = Stroke(2.dp.toPx()))
        drawCircle(
            Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value))),
            8.dp.toPx(), Offset(tx, ty),
        )
    }
}

// ── Hue bar ───────────────────────────────────────────────────────────────────

@Composable
private fun HuePicker(hue: Float, onHueChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val rainbow = remember { (0..12).map { i -> Color.hsl(i * 30f % 360f, 1f, 0.5f) } }
    Box(
        modifier = modifier
            .height(28.dp)
            .fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(14.dp))
                .semantics { contentDescription = "Hue selector, ${hue.toInt()} degrees" }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        onHueChange((down.position.x / w * 360f).coerceIn(0f, 360f))
                        drag(down.id) { change ->
                            onHueChange((change.position.x / w * 360f).coerceIn(0f, 360f))
                            change.consume()
                        }
                    }
                }
        ) {
            drawRect(brush = Brush.horizontalGradient(rainbow))
            val tx = (hue / 360f) * size.width
            val ty = size.height / 2f
            drawCircle(color = Color.Black.copy(alpha = 0.25f), radius = 13.dp.toPx(), center = Offset(tx, ty))
            drawCircle(color = Color.White,                     radius = 12.dp.toPx(), center = Offset(tx, ty))
            drawCircle(color = Color.hsl(hue % 360f, 1f, 0.5f), radius = 9.dp.toPx(),  center = Offset(tx, ty))
        }
    }
}
