package com.mapgie.dash.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LucideIcons

/**
 * A padlock and one plain sentence saying where a private item lives. The
 * overview sheets show it under the header of a private chore or task; the
 * edit sheets show it under the settings card whenever the category chosen is
 * Private, or an item that was private is being made shared, so the change of
 * home is spelled out before Save. Polite live region: it appears and changes
 * in answer to the category pick.
 */
@Composable
fun PrivateNote(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            imageVector = LucideIcons.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(14.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** The sentence under an item that is private and staying private. */
const val PRIVATE_NOTE_STAYS = "Private: kept on this phone only. It never syncs, so nobody else in the household sees it."

/** The sentence when a shared item is about to become private on Save. */
const val PRIVATE_NOTE_TO_PRIVATE = "Private: on Save this leaves the shared household list and is kept on this phone only."

/** The sentence when a private item is about to be shared on Save. */
const val PRIVATE_NOTE_TO_SHARED = "No longer private: on Save this joins the shared household list, where everyone can see it."

/**
 * Which note an edit sheet shows, or null for none: [wasPrivate] is the
 * category the sheet opened with, [isPrivateNow] the one chosen.
 */
fun privateNoteFor(wasPrivate: Boolean, isPrivateNow: Boolean): String? = when {
    wasPrivate && isPrivateNow -> PRIVATE_NOTE_STAYS
    isPrivateNow -> PRIVATE_NOTE_TO_PRIVATE
    wasPrivate -> PRIVATE_NOTE_TO_SHARED
    else -> null
}
