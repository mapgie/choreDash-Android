package com.mapgie.dash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.ui.theme.DashIcons
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.TypeAccentColors

private data class AddMenuOptionSpec(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

private fun AddMenuOption.spec(reminderLabel: String, accents: TypeAccentColors): AddMenuOptionSpec = when (this) {
    AddMenuOption.REMINDER -> AddMenuOptionSpec(
        Icons.Outlined.Notifications, reminderLabel, accents.reminderContainer, accents.onReminderContainer
    )
    AddMenuOption.CHORE -> AddMenuOptionSpec(
        DashIcons.Brush, "Chore", accents.choreContainer, accents.onChoreContainer
    )
    AddMenuOption.TASK -> AddMenuOptionSpec(
        Icons.Outlined.CheckCircle, "Task", accents.taskContainer, accents.onTaskContainer
    )
}

/**
 * The round sage add button docked in the centre slot of the bottom bar
 * (52dp per the Cozy Cream handoff; secondary is the sage role in the Cream
 * palette and stays on-palette elsewhere).
 */
@Composable
fun AddMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = { onExpandedChange(!expanded) },
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier.size(52.dp),
    ) {
        Icon(
            imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
            contentDescription = if (expanded) "Close add menu" else "Add"
        )
    }
}

/**
 * The quick-add menu that stacks centred above the bottom bar's add button
 * while it is expanded. Lives in the Scaffold FAB slot (centre position) so it
 * floats over the list content; renders nothing while collapsed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenu(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AddMenuOption) -> Unit,
    order: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    reminderLabel: String = "Reminder",
    modifier: Modifier = Modifier,
) {
    val accents = LocalTypeAccents.current
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            order.forEach { option ->
                val spec = option.spec(reminderLabel, accents)
                ExtendedFloatingActionButton(
                    onClick = {
                        onExpandedChange(false)
                        onSelect(option)
                    },
                    icon = { Icon(spec.icon, contentDescription = null) },
                    text = { Text(spec.label) },
                    containerColor = spec.containerColor,
                    contentColor = spec.contentColor
                )
            }
        }
    }
}
