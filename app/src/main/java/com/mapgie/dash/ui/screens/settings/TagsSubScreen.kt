package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.nfc.NfcWriteRequest
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.WriteTagDialog
import com.mapgie.dash.ui.components.sheet.ValueChip
import com.mapgie.dash.ui.theme.PillShape

/**
 * Settings › NFC tags: tag maintenance in one place. An Identify card reads
 * whatever tag is held to the phone (a written sticker, a blank one, a card or
 * a fob) and says what the app makes of it; an unknown one can be saved under
 * a name, a saved one attached to a chore. Below it: every saved tag with the
 * chore it logs (attach, detach, rename, forget), every chore with its tags and
 * a Write chip that stamps a new one, and every tag-alarm.
 *
 * A chore can have any number of tags; a tag logs one chore at most.
 *
 * Identify uses the activity's capture mode ([onStartNfcCapture] /
 * [nfcCapturedTagId]), the same one the memo sheet's scan uses, so a tap while
 * this page is listening is never logged as a chore or armed as an alarm.
 */
@Composable
internal fun TagsSubScreen(
    onBack: () -> Unit,
    nfcCapturedTagId: String?,
    onStartNfcCapture: () -> Unit,
    onCancelNfcCapture: () -> Unit,
    onNfcCaptureConsumed: () -> Unit,
    tagWritePending: Boolean,
    /** The pending write from Settings is an erase; the dialog says so. */
    tagErasePending: Boolean = false,
    nfcWriteResult: NfcWriteResult?,
    onStartTagWrite: (NfcWriteRequest) -> Unit,
    onCancelNfcWrite: () -> Unit,
    onNfcWriteResultConsumed: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var scanning by rememberSaveable { mutableStateOf(false) }
    // The last tag read on this page: its id, and what it resolved to (null for unknown).
    var readId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingUnlink by remember { mutableStateOf<TagEntry?>(null) }
    var confirmErase by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // Capture follows [scanning]: asked when it turns on (again after rotation,
    // which the activity forgets and saved state does not), withdrawn when it
    // turns off or the page goes away.
    LaunchedEffect(scanning) {
        if (scanning) onStartNfcCapture() else onCancelNfcCapture()
    }
    DisposableEffect(Unit) {
        onDispose { onCancelNfcCapture() }
    }
    LaunchedEffect(nfcCapturedTagId) {
        val scanned = nfcCapturedTagId ?: return@LaunchedEffect
        if (scanning) {
            scanning = false
            readId = scanned
        }
        onNfcCaptureConsumed()
    }

    val identity = readId?.let { uiState.identify(it) }
    // Dialogs: save a scanned tag, edit or forget a saved one, pick a chore to attach to.
    var savingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var forgettingId by rememberSaveable { mutableStateOf<String?>(null) }
    var attachingId by rememberSaveable { mutableStateOf<String?>(null) }

    fun savedName(tagId: String): String =
        uiState.savedTags.firstOrNull { it.nfcId == tagId }?.name ?: tagId

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = { SubScreenHeader(title = "NFC tags", onBack = onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsCaption("Scan any tag, save it with a name, and attach it to a chore now or later. A chore can have several tags; a tag logs one chore.")

            SettingsSectionLabel("Identify a tag")
            SettingsCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(vertical = 12.dp),
                ) {
                    val (title, subtitle) = when {
                        scanning -> "Listening" to "Hold a tag to the back of your phone."
                        identity == null -> "Scan a tag" to "See what it is, or save it with a name."
                        else -> identity.tagId to identity.summary
                    }
                    SettingsRowText(
                        title = title,
                        subtitle = subtitle,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        ValueChip(
                            text = if (scanning) "Cancel" else "Scan",
                            onClick = { scanning = !scanning },
                            contentDescription = if (scanning) "Stop listening for a tag" else "Scan a tag to identify it",
                            chevron = false,
                        )
                        if (!scanning && identity != null && identity.canSave) {
                            ValueChip(
                                text = "Save",
                                onClick = { savingId = identity.tagId },
                                contentDescription = "Save this tag with a name",
                                chevron = false,
                            )
                        }
                        if (!scanning && identity != null && identity.canAttach) {
                            ValueChip(
                                text = "Attach",
                                onClick = { attachingId = identity.tagId },
                                contentDescription = "Attach this tag to a chore",
                                chevron = false,
                            )
                        }
                        if (!scanning) {
                            ValueChip(
                                text = "Erase",
                                onClick = { confirmErase = true },
                                contentDescription = "Erase the next tag held to the phone",
                                chevron = false,
                            )
                        }
                    }
                }
            }
            SettingsCaption("Erase wipes whatever a tag carries so it can be written for something else. A saved tag that is erased answers with its hardware id afterwards, so scan and save it again.")

            SettingsSectionLabel("Saved tags")
            SettingsCard {
                val saved = uiState.savedTagEntries
                if (uiState.loading && uiState.savedTags.isEmpty()) {
                    SettingsCardRow(title = "Loading tags", subtitle = "From Supabase.")
                } else if (saved.isEmpty()) {
                    SettingsCardRow(
                        title = "No saved tags",
                        subtitle = if (uiState.error != null) "They couldn't be loaded." else "Scan one above and save it.",
                    )
                } else {
                    saved.forEachIndexed { index, entry ->
                        if (index > 0) SettingsHairline()
                        val tag = entry.tag
                        SettingsCardRow(
                            title = tag.name,
                            subtitle = tag.nfcId + " · " + when {
                                entry.choreName == null -> "Not attached"
                                entry.choreArchived -> "${entry.choreName} (archived)"
                                else -> entry.choreName
                            },
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (entry.choreName == null) {
                                    ValueChip(
                                        text = "Attach",
                                        onClick = { attachingId = tag.nfcId },
                                        contentDescription = "Attach ${tag.name} to a chore",
                                        chevron = false,
                                    )
                                } else {
                                    ValueChip(
                                        text = "Detach",
                                        onClick = { viewModel.detachTag(tag.nfcId) },
                                        contentDescription = "Detach ${tag.name} from ${entry.choreName}",
                                        chevron = false,
                                    )
                                }
                                ValueChip(
                                    text = "Edit",
                                    onClick = { editingId = tag.nfcId },
                                    contentDescription = "Rename or forget ${tag.name}",
                                    chevron = false,
                                )
                            }
                        }
                    }
                }
            }
            SettingsCaption("Detach keeps a tag saved, free for another chore. A tag can also be added from a chore's own edit sheet.")

            SettingsSectionLabel("Chores")
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                ChoreTagFilter.entries.forEach { f ->
                    val count = uiState.choreCount(f)
                    FilterChip(
                        selected = uiState.choreFilter == f,
                        onClick = { viewModel.setChoreFilter(f) },
                        label = {
                            Text(
                                text = if (count > 0) "${f.label} · $count" else f.label,
                                fontWeight = FontWeight.ExtraBold,
                            )
                        },
                        shape = PillShape,
                        border = null,
                        // Explicit high-contrast fills (LESSONS.md #3), as on the Memos list.
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
            SettingsCard {
                val shown = uiState.filteredChoreTags
                if (uiState.loading && uiState.chores.isEmpty()) {
                    SettingsCardRow(title = "Loading chores", subtitle = "From Supabase.")
                } else if (uiState.choreTags.isEmpty()) {
                    SettingsCardRow(title = "No chores", subtitle = if (uiState.error != null) "They couldn't be loaded." else "Add one from the Chores tab.")
                } else if (shown.isEmpty()) {
                    SettingsCardRow(
                        title = if (uiState.choreFilter == ChoreTagFilter.ON_STICKER) "No chores on a tag yet" else "Every chore is on a tag",
                        subtitle = if (uiState.choreFilter == ChoreTagFilter.ON_STICKER)
                            "Attach a saved tag above, or write a new one for a chore." else null,
                    )
                } else {
                    shown.forEachIndexed { index, entry ->
                        if (index > 0) SettingsHairline()
                        SettingsCardRow(
                            title = entry.name,
                            subtitle = entry.tags.joinToString(", ") { it.name }.ifEmpty { "No tag" } +
                                (if (entry.archived) " · archived" else ""),
                        ) {
                            ValueChip(
                                text = "Write",
                                onClick = {
                                    // A new tag with a readable id, attached once the write lands.
                                    val minted = uiState.freeTagIdFor(entry.name)
                                    onStartTagWrite(
                                        NfcWriteRequest(
                                            NfcWriteRequest.Kind.CHORE, minted,
                                            linkChore = entry.choreTagId, linkName = entry.name,
                                        )
                                    )
                                },
                                contentDescription = "Write a new tag for ${entry.name}",
                                chevron = false,
                            )
                        }
                    }
                }
            }

            SettingsSectionLabel("Tag-alarms")
            SettingsCard {
                if (uiState.tagAlarms.isEmpty()) {
                    SettingsCardRow(title = "No tag-alarms", subtitle = "Add one from the Memos tab with the Tag-alarm switch on.")
                } else {
                    uiState.tagAlarms.forEachIndexed { index, entry ->
                        if (index > 0) SettingsHairline()
                        SettingsCardRow(
                            title = entry.name,
                            subtitle = entry.tagId?.let { it + if (entry.onSticker) " · on a tag" else "" } ?: "No tag yet",
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                ValueChip(
                                    text = "Write",
                                    onClick = {
                                        val tagId = entry.tagId
                                        if (tagId != null) {
                                            onStartTagWrite(NfcWriteRequest(NfcWriteRequest.Kind.MEMO, tagId))
                                        } else {
                                            viewModel.assignTagThen(entry.ownerId, entry.name) { minted ->
                                                onStartTagWrite(NfcWriteRequest(NfcWriteRequest.Kind.MEMO, minted))
                                            }
                                        }
                                    },
                                    contentDescription = "Write ${entry.name}'s tag id to a tag",
                                    chevron = false,
                                )
                                if (entry.tagId != null) {
                                    ValueChip(
                                        text = "Unlink",
                                        onClick = { pendingUnlink = entry },
                                        contentDescription = "Unlink ${entry.name} from its tag",
                                        chevron = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SettingsCaption("Write stamps an id on a blank tag. A card that can't be written (an office pass) is scanned and saved instead. Unlink frees a tag without erasing it.")
            Spacer(Modifier.height(8.dp))
        }
    }

    // A write can attach a tag to a chore (MainActivity), so reload once it lands.
    LaunchedEffect(nfcWriteResult) {
        if (tagWritePending && nfcWriteResult == NfcWriteResult.Success) viewModel.load()
    }

    if (tagWritePending) {
        WriteTagDialog(
            result = nfcWriteResult,
            erasing = tagErasePending,
            onDismiss = {
                if (nfcWriteResult != null) onNfcWriteResultConsumed() else onCancelNfcWrite()
            }
        )
    }

    if (confirmErase) {
        AlertDialog(
            onDismissRequest = { confirmErase = false },
            title = { Text("Erase a tag?") },
            text = { Text("The next tag you hold to the phone is wiped. Whatever chore or tag-alarm it pointed at stays in the app.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmErase = false
                    onStartTagWrite(NfcWriteRequest(NfcWriteRequest.Kind.ERASE, ""))
                }) { Text("Erase", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmErase = false }) { Text("Cancel") }
            },
        )
    }

    savingId?.let { tagId ->
        TagNameDialog(
            title = "Save this tag",
            initial = "",
            confirmLabel = "Save",
            onConfirm = { name ->
                savingId = null
                viewModel.saveTag(tagId, name)
            },
            onDismiss = { savingId = null },
        )
    }

    editingId?.let { tagId ->
        TagNameDialog(
            title = "Edit tag",
            initial = savedName(tagId),
            confirmLabel = "Save",
            onConfirm = { name ->
                editingId = null
                viewModel.renameTag(tagId, name)
            },
            onDismiss = { editingId = null },
            onForget = {
                editingId = null
                forgettingId = tagId
            },
        )
    }

    forgettingId?.let { tagId ->
        AlertDialog(
            onDismissRequest = { forgettingId = null },
            title = { Text("Forget “${savedName(tagId)}”?") },
            text = { Text("Tapping it will no longer log anything, and it leaves this list. The tag itself is untouched, so it can be scanned and saved again.") },
            confirmButton = {
                TextButton(onClick = {
                    forgettingId = null
                    viewModel.forgetTag(tagId)
                }) { Text("Forget", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { forgettingId = null }) { Text("Cancel") }
            },
        )
    }

    attachingId?.let { tagId ->
        AlertDialog(
            onDismissRequest = { attachingId = null },
            title = { Text("Attach “${savedName(tagId)}” to") },
            text = {
                val chores = uiState.attachableChores
                if (chores.isEmpty()) {
                    Text("No chores yet. Add one from the Chores tab.")
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        chores.forEach { chore ->
                            Text(
                                text = chore.label,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { role = Role.Button }
                                    .clickable {
                                        attachingId = null
                                        viewModel.attachTag(tagId, chore.tagId)
                                    }
                                    .padding(vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { attachingId = null }) { Text("Cancel") }
            },
        )
    }

    pendingUnlink?.let { entry ->
        AlertDialog(
            onDismissRequest = { pendingUnlink = null },
            title = { Text("Unlink “${entry.name}”?") },
            text = { Text("Tapping its tag will no longer set this tag-alarm. The tag itself is untouched, so it can be written or linked again.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingUnlink = null
                    viewModel.unlink(entry.ownerId)
                }) { Text("Unlink", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnlink = null }) { Text("Cancel") }
            },
        )
    }
}

/** Names a tag: saving a scanned one, or renaming (and, with [onForget], forgetting) a saved one. */
@Composable
private fun TagNameDialog(
    title: String,
    initial: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onForget: (() -> Unit)? = null,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Name, like Back door") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (onForget != null) {
                    TextButton(onClick = onForget) {
                        Text("Forget this tag", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
