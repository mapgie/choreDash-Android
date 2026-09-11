package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.nfc.TagKind

/**
 * Settings › Tags: every NFC tag the app recognises, in two groups. Chore tags
 * (the `tags` table, one per chore) come first, then tag-alarm memos (a memo a
 * tap arms for the morning). Tapping a row opens an editor to rename it or
 * archive it; a tag-alarm can also release its NFC tag so the physical tag is
 * free to reuse. Nothing is permanently deleted here, matching the rest of the
 * app: archiving a chore frees its tag, releasing a memo unlinks its tag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagsSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val tags by viewModel.tags.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var editing by remember { mutableStateOf<TagRow?>(null) }

    LaunchedEffect(Unit) { viewModel.loadTags() }
    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    val choreTags = tags.filter { it.kind == TagKind.CHORE }
    val memoTags = tags.filter { it.kind == TagKind.MEMO }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { SubScreenHeader(title = "Tags", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsCaption(
                "Every NFC tag the app knows. Rename a tag, archive it, or release " +
                    "a tag-alarm so its tag is free to reuse.",
            )

            if (choreTags.isEmpty() && memoTags.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "No tags yet. Chores you register and tag-alarms you arm show up here.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }

            if (choreTags.isNotEmpty()) {
                SettingsSectionLabel("Chore tags")
                SettingsCard {
                    choreTags.forEachIndexed { index, row ->
                        if (index > 0) SettingsHairline()
                        TagNavRow(row = row, onClick = { editing = row })
                    }
                }
            }

            if (memoTags.isNotEmpty()) {
                SettingsSectionLabel("Tag-alarms")
                SettingsCard {
                    memoTags.forEachIndexed { index, row ->
                        if (index > 0) SettingsHairline()
                        TagNavRow(row = row, onClick = { editing = row })
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    editing?.let { row ->
        TagEditDialog(
            row = row,
            onRename = { newName -> viewModel.renameTag(row, newName) },
            onRelease = { viewModel.releaseTag(row) },
            onSetArchived = { archived -> viewModel.setTagArchived(row, archived) },
            onDismiss = { editing = null },
        )
    }
}

/** One tag row: name, its detail, and an "Archived" marker, opening the editor. */
@Composable
private fun TagNavRow(row: TagRow, onClick: () -> Unit) {
    val subtitle = listOfNotNull(
        "Archived".takeIf { row.archived },
        row.detail.takeIf { it.isNotBlank() },
    ).joinToString(" · ").ifBlank { null }
    SettingsNavRow(
        title = row.name,
        subtitle = subtitle,
        onClick = onClick,
    )
}

/**
 * The per-tag editor: rename, archive/unarchive, and (tag-alarms only) release
 * the NFC tag. A chore is its tag, so there is nothing to release there; archive
 * the chore to free the physical tag instead.
 */
@Composable
private fun TagEditDialog(
    row: TagRow,
    onRename: (String) -> Unit,
    onRelease: () -> Unit,
    onSetArchived: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(row.nfcId) { mutableStateOf(row.name) }
    val trimmed = draft.trim()
    val changed = trimmed.isNotBlank() && trimmed != row.name

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (row.kind == TagKind.CHORE) "Edit chore tag" else "Edit tag-alarm") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (row.kind == TagKind.MEMO) {
                    TextButton(
                        onClick = { onRelease(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Release NFC tag") }
                    SettingsCaption(
                        "Unlinks the physical tag so you can reuse it. The memo stays; " +
                            "arm it again to relink a tag.",
                    )
                }
                TextButton(
                    onClick = { onSetArchived(!row.archived); onDismiss() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(if (row.archived) "Unarchive" else "Archive") }
                if (row.kind == TagKind.CHORE) {
                    SettingsCaption("Archiving a chore frees its NFC tag to reuse.")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRename(trimmed); onDismiss() },
                enabled = changed,
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
