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
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.sheet.SheetBlock
import com.mapgie.dash.ui.components.sheet.SheetRowDivider
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * Page one of Help: the one explanation of what a chore, a task and a memo
 * are. The speed dial carries no hint text (handoff 7a): this is taught once
 * in the first-run welcome sheet and repeated under Settings › Help, so both
 * render this. The controls tour lives in [HelpGettingAround].
 *
 * [reminderLabel] is the user's chosen name for the reminders feature
 * ("Memos", "Alarms" or "Reminders").
 */
@Composable
fun HelpContent(
    reminderLabel: String,
    modifier: Modifier = Modifier,
) {
    val accents = LocalTypeAccents.current
    val one = reminderLabel.lowercase().trimEnd('s')
    SheetBlock(modifier = modifier) {
        HelpRow(
            icon = LucideIcons.HouseCheck,
            container = accents.choreContainer,
            content = accents.onChoreContainer,
            title = "Chores repeat",
            body = "A chore comes round again on its own cadence: every 3 days, every " +
                "month. Log it with a tap, a swipe, or by holding the phone to its NFC " +
                "sticker. The colour spine and badge show how overdue it is. Chores sync " +
                "across your household, so everyone sees the same list.",
        )
        SheetRowDivider()
        HelpRow(
            icon = LucideIcons.CircleCheck,
            container = accents.taskContainer,
            content = accents.onTaskContainer,
            title = "Tasks happen once",
            body = "A task is a one-off with an optional due date and priority. Tick it " +
                "when it is done and it drops into the Done section. Tasks sync across " +
                "your household too.",
        )
        SheetRowDivider()
        HelpRow(
            icon = LucideIcons.Bell,
            container = accents.reminderContainer,
            content = accents.onReminderContainer,
            title = "$reminderLabel nudge you",
            body = "A $one is a nudge at a set time, on its own or linked to a chore or " +
                "task. Snooze it or mark it done from the alert. It stays on this phone, " +
                "private to you, and never syncs.",
        )
    }
}

/**
 * Page two of Help: a grouped tour of the controls, so the buttons on a card,
 * the icons above a list, and the Settings that thin a list out are each
 * explained once, in the place they belong. Shares [HelpRow] with the type
 * cards above; the control rows tint neutrally so they read as actions rather
 * than as a fourth kind of thing.
 */
@Composable
fun HelpGettingAround(
    reminderLabel: String,
    modifier: Modifier = Modifier,
) {
    val one = reminderLabel.lowercase().trimEnd('s')
    val container = MaterialTheme.colorScheme.surfaceContainerHigh
    val content = MaterialTheme.colorScheme.onSurfaceVariant
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionLabel(text = "On a card")
        SheetBlock {
            HelpRow(
                icon = LucideIcons.CircleCheck, container = container, content = content,
                title = "Open and finish",
                body = "Tap any card to open it, then Log it (a chore) or Mark done (a task). " +
                    "Set when it happened with Just now, Earlier today, or Pick a time. " +
                    "Long-press a card to jump straight to editing.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Calendar, container = container, content = content,
                title = "Calendar",
                body = "Add the item to your phone's calendar as an event.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Pin, container = container, content = content,
                title = "Pin",
                body = "Keep the item on your home-screen widget.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Bell, container = container, content = content,
                title = "Remind",
                body = "Attach a $one so the item nudges you at a set time.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.NfcScan, container = container, content = content,
                title = "Tag",
                body = "Chores only: link an NFC sticker, then a tap of the phone logs the chore.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Undo, container = container, content = content,
                title = "History",
                body = "A chore keeps every past log. Undo reverses the last one; All opens " +
                    "the full list.",
            )
        }

        SectionLabel(text = "Around a list")
        SheetBlock {
            HelpRow(
                icon = LucideIcons.Search, container = container, content = content,
                title = "Search",
                body = "Filter the list by name, category, or owner.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.User, container = container, content = content,
                title = "Mine or everyone",
                body = "The person icon shows just your items or the whole household's.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.ArrowUp, container = container, content = content,
                title = "Sort",
                body = "The sort pill names the order in words. Tap the label to change what " +
                    "it sorts by, the arrow to flip the direction.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.LayoutGrid, container = container, content = content,
                title = "Group or flat",
                body = "Group the list under category headers, or lay it out flat.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Target, container = container, content = content,
                title = "Zen",
                body = "The target icon hides colours and counts for a calmer list. Leave it " +
                    "with the cross.",
            )
        }

        SectionLabel(text = "Adding and hiding")
        SheetBlock {
            HelpRow(
                icon = LucideIcons.Plus, container = container, content = content,
                title = "The plus button",
                body = "Adds to the page you're on. Long-press it to pick any type: a chore, " +
                    "a task, or a $one.",
            )
            SheetRowDivider()
            HelpRow(
                icon = LucideIcons.Clock, container = container, content = content,
                title = "Hide what isn't due",
                body = "Settings › Display can hide chores until they're close to due and " +
                    "tasks whose due date is far off, so the list shows only what needs doing.",
            )
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
