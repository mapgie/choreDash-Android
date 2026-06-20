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
 * When [AppTheme.CUSTOM] is selected the three hue sliders are revealed via
 * [AnimatedVisibility].
 */
@Composable
fun CompactThemePicker(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    customPrimaryHue: Float,
    customSecondaryHue: Float,
    customTertiaryHue: Float,
    onCustomHueChange: (primary: Float, secondary: Float, tertiary: Float) -> Unit,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    // All entries except CUSTOM are shown as palette cards.
    val palettes = remember { AppTheme.entries.filter { it != AppTheme.CUSTOM } }
    // Total items: standard palettes + 1 custom card
    val totalItems = palettes.size + 1
    val rowCount = (totalItems + 2) / 3 // 3 columns
    // Each card is ~80dp tall, 8dp gap between rows
    val gridHeight = (rowCount * 80 + (rowCount - 1) * 8).dp

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
                PaletteCard(
                    theme = theme,
                    isSelected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) },
                )
            }

            // Custom card at the end of the grid
            item(key = "CUSTOM") {
                PaletteCard(
                    theme = AppTheme.CUSTOM,
                    isSelected = selectedTheme == AppTheme.CUSTOM,
                    onClick = { onThemeSelected(AppTheme.CUSTOM) },
                    customPreviewColor = Color.hsl(customPrimaryHue, 0.5f, 0.45f),
                )
            }
        }

        // Custom hue sliders — only visible when CUSTOM is selected
        AnimatedVisibility(visible = selectedTheme == AppTheme.CUSTOM) {
            CustomHueControls(
                primaryHue = customPrimaryHue,
                secondaryHue = customSecondaryHue,
                tertiaryHue = customTertiaryHue,
                darkTheme = darkTheme,
                onHueChange = onCustomHueChange,
            )
        }
    }
}

@Composable
private fun PaletteCard(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    customPreviewColor: Color? = null,
) {
    val swatchColor = customPreviewColor ?: Color(theme.previewArgb.toInt())

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
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(swatchColor)
                )
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
private fun CustomHueControls(
    primaryHue: Float,
    secondaryHue: Float,
    tertiaryHue: Float,
    darkTheme: Boolean,
    onHueChange: (primary: Float, secondary: Float, tertiary: Float) -> Unit,
) {
    // Live preview swatches derived from the current hues
    val lightness = if (darkTheme) 0.70f else 0.40f
    val primaryColor   = Color.hsl(primaryHue,   0.5f, lightness)
    val secondaryColor = Color.hsl(secondaryHue, 0.4f, lightness)
    val tertiaryColor  = Color.hsl(tertiaryHue,  0.4f, lightness)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Live preview row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Preview",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                listOf(primaryColor, secondaryColor, tertiaryColor).forEach { colour ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(colour)
                    )
                }
            }

            HueSliderRow(
                label = "Primary hue",
                hue = primaryHue,
                swatchColor = primaryColor,
                onHueChange = { onHueChange(it, secondaryHue, tertiaryHue) },
            )
            HueSliderRow(
                label = "Secondary hue",
                hue = secondaryHue,
                swatchColor = secondaryColor,
                onHueChange = { onHueChange(primaryHue, it, tertiaryHue) },
            )
            HueSliderRow(
                label = "Tertiary hue",
                hue = tertiaryHue,
                swatchColor = tertiaryColor,
                onHueChange = { onHueChange(primaryHue, secondaryHue, it) },
            )
        }
    }
}

@Composable
private fun HueSliderRow(
    label: String,
    hue: Float,
    swatchColor: Color,
    onHueChange: (Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(swatchColor)
            )
            Text(
                "$label  ${hue.toInt()}°",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$label slider, ${hue.toInt()} degrees" },
        )
    }
}
