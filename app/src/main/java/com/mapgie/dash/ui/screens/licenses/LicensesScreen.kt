package com.mapgie.dash.ui.screens.licenses

// MAINTAINER NOTE: keep this list in sync with gradle/libs.versions.toml.
// Add an entry here whenever a new RUNTIME dependency is added to the project.
// Compose library versions are pinned via the Compose BOM.
//
// Excluded — not shipped in the release APK:
//   junit                   (testImplementation only)
//   hilt-android-compiler   (ksp — annotation processor, compile-time only)
//   hilt-compiler           (ksp — annotation processor, compile-time only)
//   androidx-ui-tooling     (debugImplementation only)

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private data class Library(val name: String, val copyright: String, val url: String)

private data class LicenseGroup(
    val spdxId: String,
    val title: String,
    val preamble: String,
    val url: String,
    val libraries: List<Library>
)

private val licenseGroups = listOf(
    LicenseGroup(
        spdxId = "Apache-2.0",
        title = "Apache 2.0 Licence",
        preamble = "The following libraries are included under the Apache 2.0 Licence:",
        url = "https://www.apache.org/licenses/LICENSE-2.0",
        libraries = listOf(
            Library("Kotlin", "JetBrains s.r.o.", "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt"),
            Library("AndroidX Compose BOM", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Compose Material3", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Navigation Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("Hilt", "Google LLC", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("Hilt Navigation Compose", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX WorkManager", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX DataStore Preferences", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("Ktor (OkHttp engine)", "JetBrains s.r.o.", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("KotlinX Serialization JSON", "JetBrains s.r.o.", "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt"),
            Library("KotlinX Coroutines Android", "JetBrains s.r.o.", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
            Library("AndroidX Glance", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
            Library("AndroidX Room", "The Android Open Source Project", "https://www.apache.org/licenses/LICENSE-2.0"),
        )
    ),
    LicenseGroup(
        spdxId = "MIT",
        title = "MIT Licence",
        preamble = "The following libraries are included under the MIT Licence:",
        url = "https://opensource.org/licenses/MIT",
        libraries = listOf(
            Library("Supabase postgrest-kt", "Supabase Community", "https://github.com/supabase-community/supabase-kt/blob/master/LICENSE"),
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open Source Licences") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            licenseGroups.forEach { group ->
                Text(group.title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    group.preamble,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(4.dp))
                group.libraries.forEach { lib ->
                    Column {
                        Text("• ${lib.name}", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "  Copyright © ${lib.copyright}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(
                    group.spdxId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .semantics { role = Role.Button }
                        .clickable { uriHandler.openUri(group.url) }
                        .padding(vertical = 4.dp)
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
            }
        }
    }
}
