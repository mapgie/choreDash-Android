package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.ChoreStatus
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.ReminderCard
import com.mapgie.dash.ui.components.TaskCard
import com.mapgie.dash.ui.theme.DashTheme
import com.mapgie.dash.ui.theme.LocalTypeAccents
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The Storybook analogue for the shared component layer: a set of `@Preview`s that
 * render every core component and the three card bindings side by side, in light and
 * dark, zen, and high-contrast. Reviewers get a single place to see drift, and
 * nothing here ships in the running app.
 *
 * The rule these previews guard is written up in `ui/components/README.md`:
 * **screens compose components; screens do not draw.**
 */

// ── Sample models ──────────────────────────────────────────────────────────────

private fun sampleChore(
    status: ChoreStatus,
    label: String = "Water the plants",
    owner: String? = "Alex",
    category: String? = "Kitchen",
) = Chore(
    id = "c1",
    tagId = "tag-1",
    label = label,
    category = category,
    owner = owner,
    intervalDays = 3.0,
    archivedAt = null,
    lastScanned = Instant.now().minus(2, ChronoUnit.DAYS),
    lastScanId = "s1",
    status = status,
)

private fun sampleTask(
    title: String = "Buy printer toner",
    priority: String = "normal",
    dueDate: String? = null,
    done: Boolean = false,
    owner: String? = "Sam",
    category: String? = "Errands",
) = TaskDto(
    id = "t1",
    title = title,
    notes = null,
    category = category,
    owner = owner,
    priority = priority,
    dueDate = dueDate,
    duePeriod = null,
    completedAt = if (done) Instant.now().toString() else null,
    archivedAt = null,
    reminderAt = null,
    reminded = null,
    createdAt = "",
)

private fun sampleReminder(
    subject: String = "Rotate the tyres",
    overdue: Boolean = false,
    done: Boolean = false,
    choreId: String? = null,
    taskId: String? = null,
) = ReminderDto(
    id = "r1",
    subject = subject,
    remindAt = (if (overdue) Instant.now().minusSeconds(3600) else Instant.now().plusSeconds(3600)).toString(),
    choreId = choreId,
    taskId = taskId,
    completedAt = if (done) Instant.now().toString() else null,
    reminded = false,
    createdAt = "",
    archivedAt = null,
)

// ── Showcases ──────────────────────────────────────────────────────────────────

@Composable
private fun GalleryFrame(
    dark: Boolean,
    wcag: Boolean = false,
    content: @Composable () -> Unit,
) {
    DashTheme(darkTheme = dark, wcag = wcag) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun CardsShowcase(zen: Boolean = false) {
    val accents = LocalTypeAccents.current

    DashScreenHeader("Chores", accents.choreContainer, accents.onChoreContainer)
    ChoreCard(
        chore = sampleChore(ChoreStatus.STALE),
        showOwner = true,
        zenMode = zen,
        showDueCountdown = true,
        onClick = {},
        onLongClick = {},
    )
    ChoreCard(
        chore = sampleChore(ChoreStatus.FRESH, label = "Take out recycling", owner = "Bella"),
        showOwner = true,
        zenMode = zen,
        onClick = {},
        onLongClick = {},
    )

    DashScreenHeader("Tasks", accents.taskContainer, accents.onTaskContainer)
    TaskCard(
        task = sampleTask(priority = "higher", dueDate = LocalDate.now().minusDays(1).toString()),
        onToggleDone = {},
        zenMode = zen,
        onClick = {},
        onLongClick = {},
    )
    TaskCard(
        task = sampleTask(title = "Renew library books", priority = "lower", done = true),
        onToggleDone = {},
        zenMode = zen,
        onClick = {},
        onLongClick = {},
    )

    DashScreenHeader("Memos", accents.reminderContainer, accents.onReminderContainer)
    ReminderCard(
        reminder = sampleReminder(overdue = true, choreId = "c1"),
        linkedLabel = "Chore: Water the plants",
        onClick = {},
        onToggleDone = {},
    )
    ReminderCard(
        reminder = sampleReminder(subject = "Call the dentist", taskId = "t1"),
        linkedLabel = "Task: Buy printer toner",
        onClick = {},
        onToggleDone = {},
    )
}

@Composable
private fun PrimitivesShowcase() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        listOf("Alex", "Bella", "Sam", "Kai", "Mo", "Tam").forEach { OwnerAvatar(it) }
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        CategoryBadge("Kitchen")
        SourceChip(SourceKind.CHORE, "Chore: Water plants")
        SourceChip(SourceKind.TASK, "Task: Buy toner")
    }
    ListSectionHeader("Kitchen")
    CollapsibleSectionHeader("Done", expanded = false, onToggle = {}, count = 3)
}

// ── Previews ─────────────────────────────────────────────────────────────────---

@Preview(name = "Cards / light", showBackground = true)
@Composable
private fun CardsLightPreview() = GalleryFrame(dark = false) { CardsShowcase() }

@Preview(name = "Cards / dark", showBackground = true)
@Composable
private fun CardsDarkPreview() = GalleryFrame(dark = true) { CardsShowcase() }

@Preview(name = "Cards / zen", showBackground = true)
@Composable
private fun CardsZenPreview() = GalleryFrame(dark = false) { CardsShowcase(zen = true) }

@Preview(name = "Cards / high contrast", showBackground = true)
@Composable
private fun CardsWcagPreview() = GalleryFrame(dark = false, wcag = true) { CardsShowcase() }

@Preview(name = "Primitives", showBackground = true)
@Composable
private fun PrimitivesPreview() = GalleryFrame(dark = false) { PrimitivesShowcase() }

@Preview(name = "States", showBackground = true, heightDp = 160)
@Composable
private fun StatesPreview() = GalleryFrame(dark = false) {
    Box(Modifier.height(140.dp)) { DashEmptyState("No tasks yet") }
}
