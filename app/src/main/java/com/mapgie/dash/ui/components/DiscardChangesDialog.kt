package com.mapgie.dash.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.ui.theme.textColor

/**
 * The unsaved-changes guard shown when a dirty sheet is dismissed by Back,
 * swipe-down, scrim tap or Cancel. "Keep editing" is the outlined default;
 * "Discard" is rose text. Names the item when the caller knows it.
 */
@Composable
fun DiscardChangesDialog(
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit,
    itemName: String? = null,
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("Discard changes?") },
        text = {
            Text(
                if (itemName.isNullOrBlank()) "You'll lose what you entered if you leave now."
                else "Your edits to “$itemName” won't be saved."
            )
        },
        confirmButton = {
            OutlinedButton(onClick = onKeepEditing) { Text("Keep editing") }
        },
        dismissButton = {
            TextButton(
                onClick = onDiscard,
                colors = ButtonDefaults.textButtonColors(contentColor = Swatch.ROSE.textColor()),
            ) { Text("Discard") }
        }
    )
}
