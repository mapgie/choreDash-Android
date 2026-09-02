package com.mapgie.dash.ui.components.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LucideIcons
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The offer shown at the top of an Edit sheet when a stored draft for this
 * item differs from what the sheet opened with: one soft block naming the
 * item, with Restore (applies the draft, which makes the sheet dirty) and
 * Forget (drops it). The draft is never applied on its own.
 */
@Composable
fun DraftResumeRow(
    itemName: String,
    onRestore: () -> Unit,
    onForget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SheetBlock(modifier = modifier, radius = 14.dp) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = LucideIcons.Pencil,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = "You have unsaved edits to $itemName.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                modifier = Modifier.fillMaxWidth(),
            ) {
                ValueChip(
                    text = "Restore",
                    onClick = onRestore,
                    contentDescription = "Restore unsaved edits to $itemName",
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    content = MaterialTheme.colorScheme.onSecondaryContainer,
                    chevron = false,
                )
                ValueChip(
                    text = "Forget",
                    onClick = onForget,
                    contentDescription = "Forget unsaved edits to $itemName",
                    chevron = false,
                )
            }
        }
    }
}

// ── Savers for rememberSaveable fields in the Edit sheets ────────────────────

/** Saves a nullable serializable draft as its JSON, for `rememberSaveable(stateSaver = …)`. */
fun <T> jsonStateSaver(serializer: KSerializer<T>): Saver<T?, String> = Saver(
    save = { value -> value?.let { Json.encodeToString(serializer, it) } },
    restore = { raw -> runCatching { Json.decodeFromString(serializer, raw) }.getOrNull() },
)

/** Saves an enum by name. */
inline fun <reified E : Enum<E>> enumStateSaver(): Saver<E, String> = Saver(
    save = { it.name },
    restore = { name -> enumValues<E>().firstOrNull { it.name == name } },
)

/** Saves a nullable [LocalDate] as its epoch day. */
val LocalDateStateSaver: Saver<LocalDate?, Long> = Saver(
    save = { it?.toEpochDay() },
    restore = { LocalDate.ofEpochDay(it) },
)

/** Saves a [ZonedDateTime] as epoch millis, restored in the device zone. */
val ZonedDateTimeStateSaver: Saver<ZonedDateTime, Long> = Saver(
    save = { it.toInstant().toEpochMilli() },
    restore = { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()) },
)
