package com.mapgie.dash.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.DashIcons
import com.mapgie.dash.ui.theme.Dimens

/**
 * Informational card at the foot of the Chores list reminding the user that a
 * chore can be logged by holding the phone to its NFC sticker. Not clickable;
 * the caller only shows it when the device has NFC hardware.
 */
@Composable
fun NfcHintCard(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.cardInset)
            .padding(top = 6.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 18.dp, vertical = 15.dp)
            .semantics(mergeDescendants = true) {}
    ) {
        Icon(
            imageVector = DashIcons.NfcTap,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(13.dp))
        Column {
            Text(
                text = "Tap a tag to log",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Hold your phone to a chore's NFC sticker",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
