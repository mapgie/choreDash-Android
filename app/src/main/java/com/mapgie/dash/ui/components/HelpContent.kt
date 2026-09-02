package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * The one explanation of what a chore, a task and a memo are. The speed dial
 * carries no hint text (handoff 7a): this is taught once in the first-run
 * welcome sheet and repeated under Settings › Help, so both render this.
 *
 * [reminderLabel] is the user's chosen name for the reminders feature
 * ("Memos", "Alarms" or "Reminders").
 */
@Composable
fun HelpContent(
    reminderLabel: String,
    modifier: Modifier = Modifier,
    showTips: Boolean = true,
) {
    val accents = LocalTypeAccents.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SheetBlock {
            HelpRow(
                icon = LucideIcons.HouseCheck,
                container = accents.choreContainer,
                content = accents.onChoreContainer,
                title = "Chores repeat",
                body = "A chore comes round again on its own cadence: every 3 days, every " +
                    "month. Log it with a tap, a swipe, or by holding the phone to its NFC " +
                    "sticker. The colour spine and badge show how overdue it is.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.CircleCheck,
                container = accents.taskContainer,
                content = accents.onTaskContainer,
                title = "Tasks happen once",
                body = "A task is a one-off with an optional due date and priority. Tick it " +
                    "when it is done and it drops into the Done section.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Bell,
                container = accents.reminderContainer,
                content = accents.onReminderContainer,
                title = "$reminderLabel nudge you",
                body = "A ${reminderLabel.lowercase().trimEnd('s')} is a nudge at a set time, on its " +
                    "own or linked to a chore or task. Snooze it or mark it done from the alert.",
            )
        }
        if (showTips) {
            SheetBlock {
                HelpTip("Tap a card to log or finish it. Long-press to edit.")
                SheetRowDivider()
                HelpTip("The sort pill above each list names its order in words. Tap it to change the key or direction.")
                SheetRowDivider()
                HelpTip("Zen (the target icon) hides colours and counts for a calmer list. Leave with the cross.")
                SheetRowDivider()
                HelpTip("Settings › Colours and Settings › Categories choose what tints each card and how groups are ordered.")
            }
        }
    }
}

@Composable
private fun HelpRow(
    icon: ImageVector,
    container: Color,
    content: Color,
    title: String,
    body: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(container),
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = content, modifier = Modifier.size(19.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelpTip(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
        color = LocalDashTokens.current.inkFaint,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
