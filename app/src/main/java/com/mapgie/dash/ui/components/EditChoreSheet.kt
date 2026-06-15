package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.util.CalendarShareUtils
import com.mapgie.dash.util.calendarEventWithoutTime
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditChoreSheet(
    chore: Chore,
    owners: List<String>,
    sheetState: SheetState,
    onSave: (tagId: String, label: String, owner: String?, intervalDays: Double?) -> Unit,
    onArchiveToggle: (chore: Chore, archive: Boolean) -> Unit,
    onWriteTag: (chore: Chore) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    val context = LocalContext.current
    var label by remember { mutableStateOf(chore.label) }
    var selectedOwner by remember { mutableStateOf(chore.owner ?: "") }
    var intervalText by remember { mutableStateOf(chore.intervalDays?.toInt()?.toString() ?: "") }
    var ownerExpanded by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showShareChoice by remember { mutableStateOf(false) }

    val isArchived = chore.archivedAt != null

    fun calendarInfo() = calendarEventWithoutTime(
        title = chore.label,
        description = chore.category?.let { "Category: $it" }
    )

    ModalBottomSheet(
        onDismissRequest = {
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isArchived) "Archived chore" else "Edit chore",
                    style = MaterialTheme.typography.titleMedium
                )
                Row {
                    IconButton(
                        onClick = { context.startActivity(CalendarShareUtils.buildAddToCalendarIntent(calendarInfo())) },
                        modifier = Modifier.semantics {
                            contentDescription = "Add to calendar"
                            role = Role.Button
                        }
                    ) {
                        Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    }
                    IconButton(
                        onClick = { showShareChoice = true },
                        modifier = Modifier.semantics {
                            contentDescription = "Share"
                            role = Role.Button
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                    }
                }
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            if (owners.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = ownerExpanded,
                    onExpandedChange = { ownerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOwner,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Owner") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = ownerExpanded,
                        onDismissRequest = { ownerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(none)") },
                            onClick = { selectedOwner = ""; ownerExpanded = false }
                        )
                        owners.forEach { owner ->
                            DropdownMenuItem(
                                text = { Text(owner) },
                                onClick = { selectedOwner = owner; ownerExpanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Interval (days, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedButton(
                onClick = {
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onWriteTag(chore)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Write tag")
            }

            OutlinedButton(
                onClick = { showArchiveConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (isArchived) "Unarchive" else "Archive")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val intervalDays = intervalText.toDoubleOrNull()
                        val owner = selectedOwner.ifBlank { null }
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onSave(chore.tagId, label.trim(), owner, intervalDays)
                        }
                    },
                    enabled = label.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }

    if (showShareChoice) {
        AlertDialog(
            onDismissRequest = { showShareChoice = false },
            title = { Text("Share chore") },
            text = { Text("Choose how to share \"${chore.label}\".") },
            confirmButton = {
                TextButton(onClick = {
                    showShareChoice = false
                    context.startActivity(CalendarShareUtils.buildShareIcsIntent(context, calendarInfo()))
                }) { Text("As calendar event") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showShareChoice = false
                    context.startActivity(CalendarShareUtils.buildSharePlainTextIntent(calendarInfo()))
                }) { Text("As plain text") }
            }
        )
    }

    if (showArchiveConfirm) {
        AlertDialog(
            onDismissRequest = { showArchiveConfirm = false },
            title = { Text(if (isArchived) "Unarchive chore?" else "Archive chore?") },
            text = {
                Text(
                    if (isArchived)
                        "This chore will reappear in your active list."
                    else
                        "This chore will be hidden from your active list."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showArchiveConfirm = false
                    sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                        onArchiveToggle(chore, !isArchived)
                    }
                }) { Text(if (isArchived) "Unarchive" else "Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}