package com.mapgie.dash.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.mapgie.dash.data.model.ReminderDto
import com.mapgie.dash.data.model.TagAlarmText
import java.time.Instant

/**
 * Asked right after a tag-alarm is armed while another one is already set for
 * the same morning: "Office B is also set for tomorrow 7:15 AM. Turn it off?".
 * The alarm just armed is already armed; only the others are at stake, and
 * dismissing the dialog keeps them all.
 */
@Composable
fun TagAlarmConflictDialog(
    conflicts: List<ReminderDto>,
    onTurnOff: () -> Unit,
    onKeep: () -> Unit,
) {
    val question = remember(conflicts) { TagAlarmText.conflictQuestion(conflicts, Instant.now()) }
    AlertDialog(
        onDismissRequest = onKeep,
        title = { Text(if (conflicts.size == 1) "Another tag-alarm is set" else "Other tag-alarms are set") },
        text = { Text(question) },
        confirmButton = {
            TextButton(onClick = onTurnOff) { Text(if (conflicts.size == 1) "Turn it off" else "Turn them off") }
        },
        dismissButton = {
            TextButton(onClick = onKeep) { Text(if (conflicts.size == 1) "Keep both" else "Keep all") }
        },
    )
}
