package com.mapgie.dash.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.ui.components.core.CategoryBadge
import com.mapgie.dash.ui.components.core.DashListCard
import com.mapgie.dash.ui.components.core.MetaLabel
import com.mapgie.dash.ui.theme.StatusAging
import com.mapgie.dash.ui.theme.StatusStale
import com.mapgie.dash.ui.theme.statusTone
import com.mapgie.dash.util.formatAbsoluteDate
import com.mapgie.dash.util.relativeTime

/**
 * Thin binding of a [Chore] onto the shared [DashListCard]. The shell owns the
 * container, accent bar, centring, inset, owner avatar and click plumbing; this
 * function only fills the title/category and the last-scanned/due dates.
 */
@Composable
fun ChoreCard(
    chore: Chore,
    showOwner: Boolean,
    zenMode: Boolean = false,
    showDueCountdown: Boolean = false,
    showCategory: Boolean = true,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Date text keeps carrying the freshness/overdue state, so the state is still
    // legible without relying on the bar colour alone.
    val dateColor = when (chore.status) {
        ChoreStatus.STALE, ChoreStatus.NEVER -> StatusStale
        ChoreStatus.AGING -> StatusAging
        ChoreStatus.FRESH -> MaterialTheme.colorScheme.onSurface
    }

    DashListCard(
        tone = chore.statusTone(),
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        zenMode = zenMode,
        owner = chore.owner?.takeIf { showOwner },
        trailing = {
            when {
                chore.lastScanned == null -> {
                    MetaLabel(
                        text = "Never",
                        style = MaterialTheme.typography.bodySmall,
                        italic = true
                    )
                }
                zenMode -> {
                    MetaLabel(
                        text = formatAbsoluteDate(chore.lastScanned),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                else -> {
                    if (showDueCountdown) {
                        val dueText = chore.nextDueText()
                        if (dueText != null) {
                            Text(
                                dueText,
                                style = MaterialTheme.typography.titleMedium,
                                color = dateColor
                            )
                            MetaLabel(text = relativeTime(chore.lastScanned))
                        } else {
                            MetaLabel(
                                text = relativeTime(chore.lastScanned),
                                style = MaterialTheme.typography.bodySmall,
                                color = dateColor
                            )
                        }
                    } else {
                        MetaLabel(
                            text = formatAbsoluteDate(chore.lastScanned),
                            style = MaterialTheme.typography.bodySmall,
                            color = dateColor
                        )
                        MetaLabel(text = relativeTime(chore.lastScanned))
                    }
                }
            }
        }
    ) {
        Text(
            chore.label,
            style = MaterialTheme.typography.titleMedium
        )
        if (showCategory && !zenMode && chore.category != null) {
            CategoryBadge(chore.category)
        }
    }
}
