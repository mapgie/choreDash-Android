package com.mapgie.dash.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils

/**
 * Compact theme picker showing a grid of palette cards and a custom colour section.
 *
 * Palette cards use [Role.RadioButton] semantics (mutually exclusive single-select).
 * Each card shows three 14dp colour circles: primary, secondary, tertiary for that palette.
 * When [AppTheme.CUSTOM] is selected, one row per colour role (primary, secondary,
 * tertiary, plus light/dark background overrides) is revealed via [AnimatedVisibility];
 * each row opens the shared [ColorPickerDialog] and the exact picked colour is what the
 * applied theme uses.
 */
@Composable
fun CompactThemePicker(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    customPrimaryHue: Float,
    customPrimarySaturation: Float,
    customPrimaryLightness: Float,
    customSecondaryHue: Float,
    customSecondarySaturation: Float,
    customSecondaryLightness: Float,
    customTertiaryHue: Float,
    customTertiarySaturation: Float,
    customTertiaryLightness: Float,
    onCustomHSLChange: (
        pH: Float, pS: Float, pL: Float,
        sH: Float, sS: Float, sL: Float,
        tH: Float, tS: Float, tL: Float,
    ) -> Unit,
    darkTheme: Boolean,
    customLightBackgroundArgb: Int = 0,
    customDarkBackgroundArgb: Int = 0,
    onCustomBackgroundArgbsChange: (Int, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    // All entries are shown as palette cards (including CUSTOM).
    val palettes = remember { AppTheme.entries.toList() }
    val totalItems = palettes.size
    val rowCount = (totalItems + 2) / 3 // 3 columns
    // Each card is ~80dp tall, 8dp gap between rows
    val gridHeight = (rowCount * 80 + (rowCount - 1) * 8).dp

    // Compute live custom preview colours from current HSL values
    val customPrimaryColor   = Color.hsl(customPrimaryHue,   customPrimarySaturation,   customPrimaryLightness)
    val customSecondaryColor = Color.hsl(customSecondaryHue, customSecondarySaturation, customSecondaryLightness)
    val customTertiaryColor  = Color.hsl(customTertiaryHue,  customTertiarySaturation,  customTertiaryLightness)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Colour palette",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            // Grid height is fixed so the outer scroll container handles scrolling.
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight),
        ) {
            items(palettes, key = { it.name }) { theme ->
                if (theme == AppTheme.CUSTOM) {
                    PaletteCard(
                        theme = theme,
                        isSelected = selectedTheme == AppTheme.CUSTOM,
                        onClick = { onThemeSelected(AppTheme.CUSTOM) },
                        darkTheme = darkTheme,
                        customPrimaryColor = customPrimaryColor,
                        customSecondaryColor = customSecondaryColor,
                        customTertiaryColor = customTertiaryColor,
                    )
                } else {
                    PaletteCard(
                        theme = theme,
                        isSelected = selectedTheme == theme,
                        onClick = { onThemeSelected(theme) },
                        darkTheme = darkTheme,
                    )
                }
            }
        }

        // Custom colour controls — only visible when CUSTOM is selected
        AnimatedVisibility(visible = selectedTheme == AppTheme.CUSTOM) {
            CustomColorControls(
                primaryColor   = customPrimaryColor,
                secondaryColor = customSecondaryColor,
                tertiaryColor  = customTertiaryColor,
                lightBackgroundArgb = customLightBackgroundArgb,
                darkBackgroundArgb  = customDarkBackgroundArgb,
                autoLightBackground = Color.hsl(customPrimaryHue, 0.10f, 0.98f),
                autoDarkBackground  = Color.hsl(customPrimaryHue, 0.10f, 0.10f),
                onPrimaryChange   = { h, s, l -> onCustomHSLChange(h, s, l, customSecondaryHue, customSecondarySaturation, customSecondaryLightness, customTertiaryHue, customTertiarySaturation, customTertiaryLightness) },
                onSecondaryChange = { h, s, l -> onCustomHSLChange(customPrimaryHue, customPrimarySaturation, customPrimaryLightness, h, s, l, customTertiaryHue, customTertiarySaturation, customTertiaryLightness) },
                onTertiaryChange  = { h, s, l -> onCustomHSLChange(customPrimaryHue, customPrimarySaturation, customPrimaryLightness, customSecondaryHue, customSecondarySaturation, customSecondaryLightness, h, s, l) },
                onBackgroundArgbsChange = onCustomBackgroundArgbsChange,
            )
        }
    }
}

@Composable
private fun PaletteCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    darkTheme: Boolean,
    customPrimaryColor: Color? = null,
    customSecondaryColor: Color? = null,
    customTertiaryColor: Color? = null,
) {
    // Resolve the three swatch colours for this card
    val primaryColor = when {
        theme == AppTheme.CUSTOM && customPrimaryColor != null -> customPrimaryColor
        darkTheme -> Color(theme.darkPrimaryArgb.toInt())
        else      -> Color(theme.lightPrimaryArgb.toInt())
    }
    val secondaryColor = when {
        theme == AppTheme.CUSTOM && customSecondaryColor != null -> customSecondaryColor
        darkTheme -> Color(theme.darkSecondaryArgb.toInt())
        else      -> Color(theme.lightSecondaryArgb.toInt())
    }
    val tertiaryColor = when {
        theme == AppTheme.CUSTOM && customTertiaryColor != null -> customTertiaryColor
        darkTheme -> Color(theme.darkTertiaryArgb.toInt())
        else      -> Color(theme.lightTertiaryArgb.toInt())
    }

    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(12.dp),
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
            shape = RoundedCornerShape(12.dp),
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .then(borderModifier)
            .semantics { role = Role.RadioButton }
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Three small colour circles: primary, secondary, tertiary
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    listOf(primaryColor, secondaryColor, tertiaryColor).forEach { colour ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(colour)
                        )
                    }
                }
                if (isSelected) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Text(
                text = theme.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun CustomColorControls(
    primaryColor: Color,
    secondaryColor: Color,
    tertiaryColor: Color,
    lightBackgroundArgb: Int,
    darkBackgroundArgb: Int,
    autoLightBackground: Color,
    autoDarkBackground: Color,
    onPrimaryChange: (Float, Float, Float) -> Unit,
    onSecondaryChange: (Float, Float, Float) -> Unit,
    onTertiaryChange: (Float, Float, Float) -> Unit,
    onBackgroundArgbsChange: (Int, Int) -> Unit,
) {
    // The role colours are stored as HSL, so picked ARGBs are converted back
    // before persisting; the round trip preserves the exact colour.
    fun asHsl(argb: Int, onChange: (Float, Float, Float) -> Unit) {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(argb, hsl)
        onChange(hsl[0], hsl[1], hsl[2])
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            CustomColorRow(
                label        = "Primary colour",
                color        = primaryColor,
                onArgbChange = { argb -> asHsl(argb) { h, s, l -> onPrimaryChange(h, s, l) } },
            )
            CustomColorRow(
                label        = "Secondary colour",
                color        = secondaryColor,
                onArgbChange = { argb -> asHsl(argb) { h, s, l -> onSecondaryChange(h, s, l) } },
            )
            CustomColorRow(
                label        = "Tertiary colour",
                color        = tertiaryColor,
                onArgbChange = { argb -> asHsl(argb) { h, s, l -> onTertiaryChange(h, s, l) } },
            )
            CustomColorRow(
                label        = "Background (light)",
                color        = if (lightBackgroundArgb != 0) Color(lightBackgroundArgb) else null,
                autoFallback = autoLightBackground,
                onArgbChange = { argb -> onBackgroundArgbsChange(argb, darkBackgroundArgb) },
                onReset      = { onBackgroundArgbsChange(0, darkBackgroundArgb) },
            )
            CustomColorRow(
                label        = "Background (dark)",
                color        = if (darkBackgroundArgb != 0) Color(darkBackgroundArgb) else null,
                autoFallback = autoDarkBackground,
                onArgbChange = { argb -> onBackgroundArgbsChange(lightBackgroundArgb, argb) },
                onReset      = { onBackgroundArgbsChange(lightBackgroundArgb, 0) },
            )
        }
    }
}

/**
 * One row per customisable colour. Shows the current swatch and hex, and opens
 * [ColorPickerDialog] on tap. For background rows [color] may be null, meaning
 * "Auto" (derived from the primary colour); [autoFallback] seeds the picker and
 * [onReset] restores Auto.
 */
@Composable
private fun CustomColorRow(
    label:        String,
    color:        Color?,
    onArgbChange: (Int) -> Unit,
    autoFallback: Color? = null,
    onReset:      (() -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }
    val displayColor = color ?: autoFallback ?: Color.Gray
    val isAuto       = color == null

    if (showPicker) {
        ColorPickerDialog(
            label       = label,
            currentArgb = displayColor.toArgb(),
            onDismiss   = { showPicker = false },
            onConfirm   = { newArgb ->
                onArgbChange(newArgb)
                showPicker = false
            },
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .clickable { showPicker = true }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(displayColor)
                .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
        )
        Text(
            text     = label,
            modifier = Modifier.weight(1f),
            style    = MaterialTheme.typography.bodyMedium,
            color    = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (isAuto) {
            Text(
                "Auto",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                "#%06X".format(displayColor.toArgb() and 0xFFFFFF),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (onReset != null) {
                TextButton(onClick = onReset) { Text("Auto") }
            }
        }
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
