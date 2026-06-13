package com.mapgie.dash.ui.screens.changelog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.io.BufferedReader
import java.io.InputStreamReader

private const val MAX_ENTRIES = 5

private sealed class ChangelogLine {
    data class Version(val text: String) : ChangelogLine()
    data class Section(val text: String) : ChangelogLine()
    data class Bullet(val text: String) : ChangelogLine()
}

private fun parseChangelog(raw: String): List<ChangelogLine> {
    val lines = raw.lines()
    val result = mutableListOf<ChangelogLine>()
    var entryCount = 0
    var currentBullet: StringBuilder? = null

    fun flushBullet() {
        currentBullet?.let { result.add(ChangelogLine.Bullet(it.toString().trim())) }
        currentBullet = null
    }

    for (line in lines) {
        when {
            line.startsWith("## ") -> {
                entryCount++
                if (entryCount > MAX_ENTRIES) break
                flushBullet()
                result.add(ChangelogLine.Version(line.removePrefix("## ").trim()))
            }
            line.startsWith("### ") -> {
                flushBullet()
                result.add(ChangelogLine.Section(line.removePrefix("### ").trim()))
            }
            line.startsWith("- ") -> {
                flushBullet()
                currentBullet = StringBuilder(line.removePrefix("- ").trim())
            }
            line.isBlank() -> flushBullet()
            currentBullet != null -> currentBullet?.append(" ")?.append(line.trim())
        }
    }
    flushBullet()
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val entries = remember(context) {
        val text = context.assets.open("CHANGELOG.md").use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
        parseChangelog(text)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(entries) { line ->
                when (line) {
                    is ChangelogLine.Version -> {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text(
                            line.text,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    is ChangelogLine.Section -> {
                        Text(
                            line.text,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    is ChangelogLine.Bullet -> {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "• ",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                line.text,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
