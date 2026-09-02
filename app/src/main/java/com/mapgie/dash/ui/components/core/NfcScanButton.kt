package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * The tinted round NFC button beside the "chores." title: a 34dp circle inside
 * a 44dp touch target. Tapping it opens [NfcScanDialog]; the phone is already
 * listening for tags whenever Chores is on screen, so the button's job is to
 * make that discoverable and to say what will happen.
 */
@Composable
fun NfcScanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .semantics {
                role = Role.Button
                contentDescription = "Scan an NFC tag"
            }
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(tokens.nfcButtonContainer),
        ) {
            Icon(
                imageVector = LucideIcons.NfcScan,
                contentDescription = null,
                tint = tokens.nfcButtonContent,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

/**
 * Shown while the app waits for a tag after the header button is tapped. The
 * caller dismisses it as soon as a tag arrives (the log or new-chore sheet takes
 * over). Replaces the old in-list hint card with the same guidance.
 */
@Composable
fun NfcScanDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tap a tag to log") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Hold your phone to a chore's NFC sticker. A tag the app doesn't know yet opens a new chore.",
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
