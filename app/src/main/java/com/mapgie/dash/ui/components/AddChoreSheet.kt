package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChoreSheet(
    initialTagId: String,
    owners: List<String>,
    categories: List<String>,
    sheetState: SheetState,
    onSave: (tagId: String, label: String, category: String?, owner: String?, intervalDays: Double?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetScope = rememberCoroutineScope()
    var tagId by remember { mutableStateOf(initialTagId) }
    var label by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(DEFAULT_CATEGORY) }
    var selectedOwner by remember { mutableStateOf("") }
    var intervalText by remember { mutableStateOf("") }
    var ownerExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = {
            sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
        },
        sheetState = sheetState,
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = true
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("New chore", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = tagId,
                onValueChange = { tagId = it },
                label = { Text("Tag ID") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Label") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it; if (it.isNotBlank()) categoryExpanded = true },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryEditable)
                )
                val filtered = categories.filter { it.contains(category, ignoreCase = true) }
                if (filtered.isNotEmpty()) {
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        filtered.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = { category = cat; categoryExpanded = false }
                            )
                        }
                    }
                }
            }

            if (owners.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = ownerExpanded,
                    onExpandedChange = { ownerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedOwner,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Owner") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ownerExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = ownerExpanded,
                        onDismissRequest = { ownerExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("(none)") },
                            onClick = { selectedOwner = ""; ownerExpanded = false }
                        )
                        owners.forEach { owner ->
                            DropdownMenuItem(
                                text = { Text(owner) },
                                onClick = { selectedOwner = owner; ownerExpanded = false }
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = intervalText,
                onValueChange = { intervalText = it },
                label = { Text("Interval (days, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel") }
                Button(
                    onClick = {
                        val intervalDays = intervalText.toDoubleOrNull()
                        val owner = selectedOwner.ifBlank { null }
                        val cat = category.trim().ifBlank { null }
                        sheetScope.launch { sheetState.hide() }.invokeOnCompletion {
                            onSave(tagId.trim(), label.trim(), cat, owner, intervalDays)
                        }
                    },
                    enabled = tagId.isNotBlank() && label.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Add") }
            }
        }
    }
}
