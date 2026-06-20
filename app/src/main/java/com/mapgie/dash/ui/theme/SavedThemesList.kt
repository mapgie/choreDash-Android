package com.mapgie.dash.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.dash.data.database.entities.CustomColorTheme

/**
 * Displays a list of saved custom colour themes with load, delete, and rename actions.
 *
 * - Three colour swatches are derived from each theme's HSL hues.
 * - Long-pressing a row opens a rename dialog.
 * - The active profile is indicated by a tinted surface and a label.
 * - Empty state shows a brief message.
 */
@Composable
fun SavedThemesList(
    themes: List<CustomColorTheme>,
    activeProfileId: Long,
    onLoad: (CustomColorTheme) -> Unit,
    onDelete: (CustomColorTheme) -> Unit,
    onRename: (CustomColorTheme, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var themeToDelete by remember { mutableStateOf<CustomColorTheme?>(null) }
    var themeToRename by remember { mutableStateOf<CustomColorTheme?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (themes.isEmpty()) {
            Text(
                "No saved themes yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            themes.forEach { theme ->
                SavedThemeRow(
                    theme = theme,
                    isActive = theme.id == activeProfileId,
                    onLoad = { onLoad(theme) },
                    onDeleteRequest = { themeToDelete = theme },
                    onLongPress = { themeToRename = theme },
                )
            }
        }
    }

    // Delete confirmation dialog
    themeToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { themeToDelete = null },
            title = { Text("Delete theme") },
            text = { Text("Delete \"${target.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(target)
                        themeToDelete = null
                    },
                    modifier = Modifier.semantics { role = Role.Button },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { themeToDelete = null },
                    modifier = Modifier.semantics { role = Role.Button },
                ) {
                    Text("Cancel")
                }
            },
        )
    }

    // Rename dialog
    themeToRename?.let { target ->
        RenameDialog(
            currentName = target.name,
            onConfirm = { newName ->
                onRename(target, newName)
                themeToRename = null
            },
            onDismiss = { themeToRename = null },
        )
    }
}

@Composable
private fun SavedThemeRow(
    theme: CustomColorTheme,
    isActive: Boolean,
    onLoad: () -> Unit,
    onDeleteRequest: () -> Unit,
    onLongPress: () -> Unit,
) {
    val primaryColor   = Color.hsl(theme.primaryHue,   0.5f, 0.45f)
    val secondaryColor = Color.hsl(theme.secondaryHue, 0.4f, 0.45f)
    val tertiaryColor  = Color.hsl(theme.tertiaryHue,  0.4f, 0.45f)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { role = Role.Button }
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
                onLongClickLabel = "Rename ${theme.name}",
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Three colour swatches
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(primaryColor, secondaryColor, tertiaryColor).forEach { colour ->
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(colour)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Theme name + active indicator
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isActive)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isActive) {
                    Text(
                        "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // Rename icon button
            IconButton(
                onClick = onLongPress,
                modifier = Modifier.semantics { contentDescription = "Rename ${theme.name}" },
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = null,
                    tint = if (isActive)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Load icon button
            IconButton(
                onClick = onLoad,
                modifier = Modifier.semantics { contentDescription = "Load ${theme.name}" },
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (isActive)
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else
                        MaterialTheme.colorScheme.primary,
                )
            }

            // Delete icon button
            IconButton(
                onClick = onDeleteRequest,
                modifier = Modifier.semantics { contentDescription = "Delete ${theme.name}" },
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun RenameDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename theme") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Theme name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank(),
                modifier = Modifier.semantics { role = Role.Button },
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { role = Role.Button },
            ) {
                Text("Cancel")
            }
        },
    )
}
