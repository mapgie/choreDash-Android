package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics

/**
 * Shown when pinning a task/chore while 2+ Pinned Item widgets are placed, since a
 * single tap can no longer unambiguously target "the" widget. Widgets have no
 * user-given label yet (no config screen), so they're offered in placement order.
 */
@Composable
fun PinWidgetChooserDialog(
    widgetIds: List<Int>,
    onChoose: (appWidgetId: Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pin to which widget?") },
        text = {
            Column {
                widgetIds.forEachIndexed { index, appWidgetId ->
                    TextButton(
                        onClick = { onChoose(appWidgetId) },
                        modifier = Modifier.semantics { role = Role.Button }
                    ) {
                        Text("Pinned Widget ${index + 1}")
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
