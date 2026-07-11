package com.mapgie.dash.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.ui.theme.TypeChoreContainer
import com.mapgie.dash.ui.theme.TypeChoreOnContainer
import com.mapgie.dash.ui.theme.TypeReminderContainer
import com.mapgie.dash.ui.theme.TypeReminderOnContainer
import com.mapgie.dash.ui.theme.TypeTaskContainer
import com.mapgie.dash.ui.theme.TypeTaskOnContainer

private data class AddMenuOptionSpec(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

private fun AddMenuOption.spec(reminderLabel: String): AddMenuOptionSpec = when (this) {
    AddMenuOption.REMINDER -> AddMenuOptionSpec(
        Icons.Filled.Notifications, reminderLabel, TypeReminderContainer, TypeReminderOnContainer
    )
    AddMenuOption.CHORE -> AddMenuOptionSpec(
        Icons.Filled.CleaningServices, "Chore", TypeChoreContainer, TypeChoreOnContainer
    )
    AddMenuOption.TASK -> AddMenuOptionSpec(
        Icons.Filled.CheckCircle, "Task", TypeTaskContainer, TypeTaskOnContainer
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AddMenuOption) -> Unit,
    order: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    reminderLabel: String = "Reminder",
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.End, modifier = modifier) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                order.forEach { option ->
                    val spec = option.spec(reminderLabel)
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
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "Close add menu" else "Add"
            )
        }
    }
}
