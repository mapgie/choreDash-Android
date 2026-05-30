package com.mapgie.dash.ui.screens.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mapgie.dash.data.model.TaskDto
import com.mapgie.dash.ui.components.EditTaskSheet
import com.mapgie.dash.ui.components.TaskCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskDto?>(null) }
    var doneExpanded by remember { mutableStateOf(false) }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { viewModel.setSort(uiState.sort.next()) }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort: ${uiState.sort.label}")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingTask = null; showTaskSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Add task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Filter row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TaskFilter.entries.forEach { f ->
                    FilterChip(
                        selected = uiState.filter == f,
                        onClick = { viewModel.setFilter(f) },
                        label = { Text(f.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
                Spacer(Modifier.weight(1f))
                FilterChip(
                    selected = uiState.groupByCategory,
                    onClick = { viewModel.setGroupBy(!uiState.groupByCategory) },
                    label = { Text("Group") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                    )
                )
            }

            // Owner filter row (only when ownerHandle is set)
            if (uiState.ownerHandle.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OwnerFilter.entries.forEach { f ->
                        FilterChip(
                            selected = uiState.ownerFilter == f,
                            onClick = { viewModel.setOwnerFilter(f) },
                            label = { Text(f.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiary,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiary
                            )
                        )
                    }
                }
            }

            if (uiState.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val active = uiState.activeTasks
            val done = uiState.doneTasks

            if (active.isEmpty() && done.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No tasks",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                return@Column
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.groupByCategory && uiState.filter != TaskFilter.DONE) {
                    val grouped = active.groupBy { it.category?.takeIf(String::isNotBlank) ?: "Other" }
                    grouped.forEach { (cat, tasks) ->
                        item(key = "header_$cat") {
                            Text(
                                text = cat.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                        items(tasks, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { editingTask = task; showTaskSheet = true },
                                onToggleDone = { viewModel.markDone(task.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                } else {
                    items(active, key = { it.id }) { task ->
                        TaskCard(
                            task = task,
                            onClick = { editingTask = task; showTaskSheet = true },
                            onToggleDone = { viewModel.markDone(task.id) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }

                if (done.isNotEmpty()) {
                    item(key = "done_header") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Done (${done.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { doneExpanded = !doneExpanded }) {
                                Icon(
                                    if (doneExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = if (doneExpanded) "Collapse" else "Expand"
                                )
                            }
                        }
                    }
                    if (doneExpanded) {
                        items(done, key = { it.id }) { task ->
                            TaskCard(
                                task = task,
                                onClick = { editingTask = task; showTaskSheet = true },
                                onToggleDone = { viewModel.markUndone(task.id) },
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showTaskSheet) {
        EditTaskSheet(
            task = editingTask,
            owners = uiState.owners,
            categories = uiState.categories,
            onSave = { insert -> viewModel.addTask(insert) },
            onUpdate = { update -> editingTask?.id?.let { viewModel.updateTask(it, update) } },
            onDelete = { editingTask?.id?.let { viewModel.deleteTask(it) } },
            onDismiss = { showTaskSheet = false; editingTask = null }
        )
    }
}

private val TaskFilter.label: String
    get() = when (this) {
        TaskFilter.ALL -> "All"
        TaskFilter.ACTIVE -> "Active"
        TaskFilter.DONE -> "Done"
    }

private val OwnerFilter.label: String
    get() = when (this) {
        OwnerFilter.ALL -> "All"
        OwnerFilter.MINE -> "Mine"
        OwnerFilter.UNASSIGNED -> "Unassigned"
    }

private val TaskSort.label: String
    get() = when (this) {
        TaskSort.PRIORITY -> "Priority"
        TaskSort.DUE -> "Due date"
        TaskSort.CREATED -> "Created"
    }

private fun TaskSort.next(): TaskSort = when (this) {
    TaskSort.PRIORITY -> TaskSort.DUE
    TaskSort.DUE -> TaskSort.CREATED
    TaskSort.CREATED -> TaskSort.PRIORITY
}
