package com.mapgie.dash.ui.screens.licenses

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

private data class License(val name: String, val version: String, val spdx: String, val url: String)

private val LICENSES = listOf(
    License("Kotlin", "2.0.21", "Apache-2.0", "https://github.com/JetBrains/kotlin/blob/master/license/LICENSE.txt"),
    License("Jetpack Compose BOM", "2024.06.00", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("Material3 for Compose", "1.2.1", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("AndroidX Navigation Compose", "2.7.7", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("Hilt", "2.52", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("Hilt Navigation Compose", "1.2.0", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("WorkManager", "2.9.1", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("DataStore Preferences", "1.1.1", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("Supabase postgrest-kt", "2.6.1", "MIT", "https://github.com/supabase-community/supabase-kt/blob/master/LICENSE"),
    License("Ktor (OkHttp engine)", "2.3.12", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0"),
    License("kotlinx.serialization JSON", "1.7.3", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt"),
    License("kotlinx.coroutines", "1.8.1", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt"),
    License("Jetpack Glance", "1.1.1", "Apache-2.0", "https://www.apache.org/licenses/LICENSE-2.0")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open-source licenses") },
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
        val uriHandler = LocalUriHandler.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            items(LICENSES) { lic ->
                ListItem(
                    headlineContent = { Text(lic.name) },
                    supportingContent = {
                        Text(
                            "${lic.version} · ",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingContent = {
                        Text(
                            text = lic.spdx,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .semantics { role = Role.Button }
                                .clickable { uriHandler.openUri(lic.url) }
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}
