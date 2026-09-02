package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/**
 * Wraps a settings sub-screen in a Scaffold whose top bar is the 4a header:
 * the accent strip, a back chevron and the lowercase serif title with an
 * accent full stop ("appearance."), flat on the page ground.
 */
@Composable
fun SettingsSubScreenScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = { SubScreenHeader(title = title, onBack = onBack) },
        content = content,
    )
}

/**
 * A single version entry parsed from CHANGELOG.md, with its header line
 * (e.g. "[0.4.0] - 2026-06-14") and the Markdown body that follows it.
 */
data class ChangelogEntry(val header: String, val body: String)

/**
 * Parses CHANGELOG.md content and returns up to [maxEntries] of the most
 * recent version entries (lines starting with "## ["), in file order.
 */
fun parseChangelog(content: String, maxEntries: Int = 5): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var currentHeader: String? = null
    val currentBody = StringBuilder()

    for (line in content.lines()) {
        when {
            line.startsWith("## [") -> {
                currentHeader?.let { entries.add(ChangelogEntry(it, currentBody.toString().trimEnd())) }
                if (entries.size >= maxEntries) break
                currentHeader = line.removePrefix("## ")
                currentBody.clear()
            }
            currentHeader != null && line.trimEnd() != "---" -> currentBody.appendLine(line)
        }
    }
    if (currentHeader != null && entries.size < maxEntries) {
        entries.add(ChangelogEntry(currentHeader, currentBody.toString().trimEnd()))
    }
    return entries.take(maxEntries)
}

/**
 * Renders a small Markdown subset used by the changelog: `### ` headings,
 * `- `/`* ` bullet rows, `**bold**` inline spans, and blank-line spacing.
 */
@Composable
fun ChangelogBody(body: String) {
    val lines = body.lines()
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("### ") -> {
                Text(
                    line.removePrefix("### ").trim(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
            line.startsWith("- ") || line.startsWith("* ") -> {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "• ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        renderInlineMarkdown(line.removePrefix("- ").removePrefix("* ").trim()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            line.isBlank() -> Spacer8()
            else -> {
                Text(
                    renderInlineMarkdown(line.trim()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        i++
    }
}

@Composable
private fun Spacer8() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 4.dp))
}

/**
 * Renders `**bold**` spans within [text] as an [androidx.compose.ui.text.AnnotatedString].
 */
fun renderInlineMarkdown(text: String) = buildAnnotatedString {
    var remaining = text
    while (true) {
        val start = remaining.indexOf("**")
        if (start == -1) {
            append(remaining)
            break
        }
        val end = remaining.indexOf("**", start + 2)
        if (end == -1) {
            append(remaining)
            break
        }
        append(remaining.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(remaining.substring(start + 2, end))
        }
        remaining = remaining.substring(end + 2)
    }
}

/**
 * "What's New" dialog showing the most recent changelog entries, parsed from
 * `assets/CHANGELOG.md`. Falls back to a friendly message if the asset is
 * missing or unparsable.
 */
@Composable
fun ChangelogDialog(
    entries: List<ChangelogEntry>,
    onDismiss: () -> Unit,
    onViewFullChangelog: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("What's New") },
        text = {
            if (entries.isEmpty()) {
                Text("No changelog available.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxHeight(0.7f)
                        .verticalScroll(rememberScrollState())
                ) {
                    entries.forEachIndexed { index, entry ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                        Text(
                            entry.header,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        ChangelogBody(entry.body)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onViewFullChangelog) { Text("View full changelog") }
        }
    )
}
