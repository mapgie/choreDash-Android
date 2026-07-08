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
import androidx.compose.ui.unit.dp

enum class AddMenuOption { CHORE, TASK, REMINDER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMenuFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AddMenuOption) -> Unit,
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
                ExtendedFloatingActionButton(
                    onClick = {
                        onExpandedChange(false)
                        onSelect(AddMenuOption.REMINDER)
                    },
                    icon = { Icon(Icons.Filled.Notifications, contentDescription = null) },
                    text = { Text("One-off reminder") }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        onExpandedChange(false)
                        onSelect(AddMenuOption.CHORE)
                    },
                    icon = { Icon(Icons.Filled.CleaningServices, contentDescription = null) },
                    text = { Text("Recurring chore") }
                )
                ExtendedFloatingActionButton(
                    onClick = {
                        onExpandedChange(false)
                        onSelect(AddMenuOption.TASK)
                    },
                    icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null) },
                    text = { Text("One-time task") }
                )
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
