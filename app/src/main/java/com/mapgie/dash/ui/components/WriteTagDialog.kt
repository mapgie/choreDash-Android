package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.dash.nfc.NfcWriteResult

/**
 * Shown while the app waits for a tag to be tapped to write a chore's tag ID to it,
 * and reports the outcome once a tag is scanned.
 */
@Composable
fun WriteTagDialog(
    result: NfcWriteResult?,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write tag") },
        text = {
            when (result) {
                null -> Row {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Hold the NFC tag near the back of your phone.",
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                }
                NfcWriteResult.Success -> Text(
                    "Tag written successfully.",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                NfcWriteResult.NotWritable -> Text(
                    "This tag can't be written to. Try a different tag.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
                NfcWriteResult.TooSmall -> Text(
                    "This tag doesn't have enough storage. Try a different tag.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
                is NfcWriteResult.Error -> Text(
                    "Couldn't write to the tag. Please try again.",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (result == null) "Cancel" else "OK")
            }
        }
    )
}
