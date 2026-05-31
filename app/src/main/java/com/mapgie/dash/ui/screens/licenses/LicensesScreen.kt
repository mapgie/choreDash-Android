package com.mapgie.dash.ui.screens.licenses

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
import androidx.compose.ui.unit.dp

private data class License(val name: String, val version: String, val spdx: String, val url: String)

private val LICENSES = listOf(
    License("Kotlin", "2.0.21", "Apache-2.0", "https://github.com/JetBrains/kotlin"),
    License("Jetpack Compose BOM", "2024.06.00", "Apache-2.0", "https://developer.android.com/jetpack/compose"),
    License("Material3 for Compose", "1.2.1", "Apache-2.0", "https://m3.material.io"),
    License("AndroidX Navigation Compose", "2.7.7", "Apache-2.0", "https://developer.android.com/jetpack/compose/navigation"),
    License("Hilt", "2.52", "Apache-2.0", "https://dagger.dev/hilt/"),
    License("Hilt Navigation Compose", "1.2.0", "Apache-2.0", "https://developer.android.com/jetpack/compose/libraries#hilt"),
    License("WorkManager", "2.9.1", "Apache-2.0", "https://developer.android.com/topic/libraries/architecture/workmanager"),
    License("DataStore Preferences", "1.1.1", "Apache-2.0", "https://developer.android.com/jetpack/datastore"),
    License("Supabase postgrest-kt", "2.6.1", "MIT", "https://github.com/supabase-community/supabase-kt"),
    License("Ktor (OkHttp engine)", "2.3.12", "Apache-2.0", "https://ktor.io"),
    License("kotlinx.serialization", "1.7.3", "Apache-2.0", "https://github.com/Kotlin/kotlinx.serialization"),
    License("kotlinx.coroutines", "1.8.1", "Apache-2.0", "https://github.com/Kotlin/kotlinx.coroutines")
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
                            "${lic.version} · ${lic.spdx}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}