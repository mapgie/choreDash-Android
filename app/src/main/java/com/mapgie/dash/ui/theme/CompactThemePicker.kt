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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Compact theme picker showing a grid of palette cards and a custom HSL section.
 *
 * Palette cards use [Role.RadioButton] semantics (mutually exclusive single-select).
 * Each card shows three 14dp colour circles: primary, secondary, tertiary for that palette.
 * When [AppTheme.CUSTOM] is selected the full HSL controls are revealed via
 * [AnimatedVisibility].
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

        // Custom HSL controls — only visible when CUSTOM is selected
        AnimatedVisibility(visible = selectedTheme == AppTheme.CUSTOM) {
            CustomHSLControls(
                primaryHue = customPrimaryHue,
                primarySaturation = customPrimarySaturation,
                primaryLightness = customPrimaryLightness,
                secondaryHue = customSecondaryHue,
                secondarySaturation = customSecondarySaturation,
                secondaryLightness = customSecondaryLightness,
                tertiaryHue = customTertiaryHue,
                tertiarySaturation = customTertiarySaturation,
                tertiaryLightness = customTertiaryLightness,
                onHSLChange = onCustomHSLChange,
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
private fun CustomHSLControls(
    primaryHue: Float,
    primarySaturation: Float,
    primaryLightness: Float,
    secondaryHue: Float,
    secondarySaturation: Float,
    secondaryLightness: Float,
    tertiaryHue: Float,
    tertiarySaturation: Float,
    tertiaryLightness: Float,
    onHSLChange: (
        pH: Float, pS: Float, pL: Float,
        sH: Float, sS: Float, sL: Float,
        tH: Float, tS: Float, tL: Float,
    ) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Primary colour group
            HSLRoleGroup(
                label = "Primary colour",
                hue = primaryHue,
                saturation = primarySaturation,
                lightness = primaryLightness,
                onHueChange        = { onHSLChange(it, primarySaturation, primaryLightness, secondaryHue, secondarySaturation, secondaryLightness, tertiaryHue, tertiarySaturation, tertiaryLightness) },
                onSaturationChange = { onHSLChange(primaryHue, it, primaryLightness, secondaryHue, secondarySaturation, secondaryLightness, tertiaryHue, tertiarySaturation, tertiaryLightness) },
                onLightnessChange  = { onHSLChange(primaryHue, primarySaturation, it, secondaryHue, secondarySaturation, secondaryLightness, tertiaryHue, tertiarySaturation, tertiaryLightness) },
            )
            // Secondary colour group
            HSLRoleGroup(
                label = "Secondary colour",
                hue = secondaryHue,
                saturation = secondarySaturation,
                lightness = secondaryLightness,
                onHueChange        = { onHSLChange(primaryHue, primarySaturation, primaryLightness, it, secondarySaturation, secondaryLightness, tertiaryHue, tertiarySaturation, tertiaryLightness) },
                onSaturationChange = { onHSLChange(primaryHue, primarySaturation, primaryLightness, secondaryHue, it, secondaryLightness, tertiaryHue, tertiarySaturation, tertiaryLightness) },
                onLightnessChange  = { onHSLChange(primaryHue, primarySaturation, primaryLightness, secondaryHue, secondarySaturation, it, tertiaryHue, tertiarySaturation, tertiaryLightness) },
            )
            // Tertiary colour group
            HSLRoleGroup(
                label = "Tertiary colour",
                hue = tertiaryHue,
                saturation = tertiarySaturation,
                lightness = tertiaryLightness,
                onHueChange        = { onHSLChange(primaryHue, primarySaturation, primaryLightness, secondaryHue, secondarySaturation, secondaryLightness, it, tertiarySaturation, tertiaryLightness) },
                onSaturationChange = { onHSLChange(primaryHue, primarySaturation, primaryLightness, secondaryHue, secondarySaturation, secondaryLightness, tertiaryHue, it, tertiaryLightness) },
                onLightnessChange  = { onHSLChange(primaryHue, primarySaturation, primaryLightness, secondaryHue, secondarySaturation, secondaryLightness, tertiaryHue, tertiarySaturation, it) },
            )
        }
    }
}

/**
 * A grouped set of HSL sliders for one colour role (primary, secondary, or tertiary).
 * Shows a live-preview swatch circle and three labelled sliders.
 */
@Composable
private fun HSLRoleGroup(
    label: String,
    hue: Float,
    saturation: Float,
    lightness: Float,
    onHueChange: (Float) -> Unit,
    onSaturationChange: (Float) -> Unit,
    onLightnessChange: (Float) -> Unit,
) {
    val previewColor = Color.hsl(hue, saturation, lightness)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section label + live preview swatch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(previewColor)
            )
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Hue slider
        HSLSliderRow(
            label = "Hue ${hue.toInt()}°",
            value = hue,
            valueRange = 0f..360f,
            onValueChange = onHueChange,
            contentDescription = "$label hue slider, ${hue.toInt()} degrees",
        )
        // Saturation slider
        HSLSliderRow(
            label = "Saturation ${(saturation * 100).toInt()}%",
            value = saturation,
            valueRange = 0f..1f,
            onValueChange = onSaturationChange,
            contentDescription = "$label saturation slider, ${(saturation * 100).toInt()} percent",
        )
        // Lightness slider
        HSLSliderRow(
            label = "Lightness ${(lightness * 100).toInt()}%",
            value = lightness,
            valueRange = 0f..1f,
            onValueChange = onLightnessChange,
            contentDescription = "$label lightness slider, ${(lightness * 100).toInt()} percent",
        )
    }
}

@Composable
private fun HSLSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    contentDescription: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { this.contentDescription = contentDescription },
        )
    }
}
