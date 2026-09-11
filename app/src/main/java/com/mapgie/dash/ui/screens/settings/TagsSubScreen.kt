package com.mapgie.dash.ui.screens.settings

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.nfc.NfcWriteRequest
import com.mapgie.dash.nfc.NfcWriteResult
import com.mapgie.dash.ui.components.WriteTagDialog
import com.mapgie.dash.ui.components.sheet.ValueChip

/**
 * Settings › NFC tags: tag maintenance in one place. An Identify card reads
 * whatever tag is held to the phone and says what the app makes of it (a
 * chore, a tag-alarm, or nothing). Below it, every chore's tag and every
 * tag-alarm, each with a Write chip that stamps its id on a blank sticker
 * through the same write dialog the chore sheet uses; a tag-alarm can also be
 * unlinked, or given an id here when it has none.
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

    val readEntry = readId?.let { uiState.identify(it) }

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
            SettingsCaption("Every tag the app recognises. A tag has one job: it belongs to one chore or one tag-alarm.")

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
                        readId == null -> "Scan a tag" to "See which chore or tag-alarm it belongs to."
                        readEntry == null -> readId!! to "Not linked to anything. Open a chore or tag-alarm to link it."
                        else -> readId!! to "${readEntry.kind.label}: ${readEntry.name}" +
                            if (readEntry.archived) " (archived)" else ""
                    }
                    SettingsRowText(
                        title = title,
                        subtitle = subtitle,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    ValueChip(
                        text = if (scanning) "Cancel" else "Scan",
                        onClick = { scanning = !scanning },
                        contentDescription = if (scanning) "Stop listening for a tag" else "Scan a tag to identify it",
                        chevron = false,
                    )
                }
            }

            SettingsSectionLabel("Chores")
            SettingsCard {
                if (uiState.loading && uiState.chores.isEmpty()) {
                    SettingsCardRow(title = "Loading chores", subtitle = "From Supabase.")
                } else if (uiState.choreTags.isEmpty()) {
                    SettingsCardRow(title = "No chores", subtitle = if (uiState.error != null) "They couldn't be loaded." else "Add one from the Chores tab.")
                } else {
                    uiState.choreTags.forEachIndexed { index, entry ->
                        if (index > 0) SettingsHairline()
                        SettingsCardRow(
                            title = entry.name,
                            subtitle = entry.tagId + if (entry.archived) " · archived" else "",
                        ) {
                            ValueChip(
                                text = "Write",
                                onClick = { onStartTagWrite(NfcWriteRequest(NfcWriteRequest.Kind.CHORE, entry.ownerId)) },
                                contentDescription = "Write ${entry.name}'s id to a tag",
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
                            subtitle = entry.tagId ?: "No tag yet",
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
            SettingsCaption("Write stamps the id on a blank sticker. A card that can't be written (an office pass) is linked by scanning it from the tag-alarm's own sheet instead.")
            Spacer(Modifier.height(8.dp))
        }
    }

    if (tagWritePending) {
        WriteTagDialog(
            result = nfcWriteResult,
            onDismiss = {
                if (nfcWriteResult != null) onNfcWriteResultConsumed() else onCancelNfcWrite()
            }
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
