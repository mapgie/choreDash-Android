package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens

/**
 * Cozy Cream section label: small caps, extra-bold, widely letterspaced and
 * deliberately faint. One style for every list group header and settings
 * section ("KITCHEN", "APPEARANCE", "OVERDUE") so grouping reads the same on
 * every screen.
 */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.5.sp,
            letterSpacing = 1.6.sp,
        ),
        color = color,
        modifier = modifier,
    )
}

/**
 * A list group header: the [SectionLabel] on the left and, when given, the
 * group's item [count] on the right in the faint section-count ink. The count
 * is spoken as "N items" so it is never a bare number to TalkBack.
 */
@Composable
fun SectionHeaderRow(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionLabel(text = text, color = color)
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.5.sp,
                ),
                color = LocalDashTokens.current.sectionCount,
                modifier = Modifier.clearAndSetSemantics {
                    contentDescription = if (count == 1) "1 item" else "$count items"
                },
            )
        }
    }
}
