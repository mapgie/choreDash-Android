package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons

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
 *
 * When [collapsed] is non-null the header is collapsible: a chevron follows the
 * count, pointing down when the group is open and right when it is collapsed.
 * The caller owns the click (and its `Role.Button` semantics).
 */
@Composable
fun SectionHeaderRow(
    text: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    collapsed: Boolean? = null,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth(),
    ) {
        SectionLabel(text = text, color = color)
        Row(verticalAlignment = Alignment.Bottom) {
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
            if (collapsed != null) {
                Icon(
                    imageVector = if (collapsed) LucideIcons.ChevronRight else LucideIcons.ChevronDown,
                    contentDescription = null,
                    tint = LocalDashTokens.current.sectionCount,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp),
                )
            }
        }
    }
}
