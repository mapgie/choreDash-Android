package com.mapgie.dash.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mapgie.dash.data.model.CategoryCatalog
import com.mapgie.dash.data.model.CategoryIcon
import com.mapgie.dash.data.model.CategoryStyle
import com.mapgie.dash.data.model.Chore
import com.mapgie.dash.data.model.GENERAL_CATEGORY
import com.mapgie.dash.data.model.Swatch
import com.mapgie.dash.data.model.TagDto
import com.mapgie.dash.ui.components.ChoreCard
import com.mapgie.dash.ui.components.NewCategoryDialog
import com.mapgie.dash.ui.components.core.CardIconChip
import com.mapgie.dash.ui.components.core.MetaCaption
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.components.sheet.ValueChip
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.isDarkScheme
import com.mapgie.dash.ui.theme.textColor
import com.mapgie.dash.ui.theme.tintColor
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt

/**
 * Settings › Categories (handoff 5a): every category in use (plus any created
 * here), draggable to reorder, with "+" in the header to add one. Tapping a row
 * expands it inline: rename field, icon picker, colour picker, Delete (which
 * says where the items go) and Done. General is the default category: it is
 * always last, cannot be renamed, deleted or reordered, but can be styled.
 *
 * Reordering also works without a drag: the expanded editor has Move up / Move
 * down chips so the order is reachable with TalkBack and a keyboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategoriesSubScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val catalog by viewModel.catalog.collectAsState()
    val usage by viewModel.categoryUsage.collectAsState()
    val saveError by viewModel.saveError.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val tokens = LocalDashTokens.current

    var expanded by remember { mutableStateOf<String?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadCategoryUsage() }
    LaunchedEffect(saveError) {
        saveError?.let {
            snackbarHost.showSnackbar(it)
            viewModel.clearSaveError()
        }
    }

    val inUse = usage.keys
    val categories = catalog.allCategories(inUse)
    val reorderable = categories.filterNot { it.equals(GENERAL_CATEGORY, ignoreCase = true) }

    fun usageFor(name: String): CategoryUsage =
        usage.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value ?: CategoryUsage()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            SubScreenHeader(
                title = "Categories",
                onBack = onBack,
                actions = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .semantics {
                                role = Role.Button
                                contentDescription = "Add category"
                            }
                            .clickable { showAdd = true },
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                        ) {
                            Icon(
                                LucideIcons.Plus,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                "Drag to reorder. Tap a category to rename it, change its icon or colour.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = tokens.inkFaint,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            )

            ReorderableCategoryList(
                names = reorderable,
                catalog = catalog,
                expanded = expanded,
                usageFor = ::usageFor,
                onExpand = { expanded = if (expanded == it) null else it },
                onReorder = { viewModel.setCategoryOrder(it) },
                onStyle = { name, style -> viewModel.setCategoryStyle(name, style) },
                onRename = { from, to -> viewModel.renameCategory(from, to); expanded = to },
                onDelete = { pendingDelete = it },
            )

            CategoryRow(
                name = GENERAL_CATEGORY,
                subtitle = "Default · can't be deleted",
                catalog = catalog,
                isExpanded = expanded.equals(GENERAL_CATEGORY, ignoreCase = true),
                reorderable = false,
                onExpand = { expanded = if (expanded.equals(GENERAL_CATEGORY, ignoreCase = true)) null else GENERAL_CATEGORY },
                onStyle = { style -> viewModel.setCategoryStyle(GENERAL_CATEGORY, style) },
                onRename = null,
                onDelete = null,
                onMoveUp = null,
                onMoveDown = null,
                modifier = Modifier.alpha(0.85f),
            )
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showAdd) {
        NewCategoryDialog(
            onCreate = { name ->
                viewModel.addCategory(name)
                showAdd = false
                expanded = name.trim()
            },
            onDismiss = { showAdd = false },
        )
    }

    pendingDelete?.let { name ->
        val count = usageFor(name)
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete “$name”?") },
            text = {
                Text(
                    if (count.total == 0) "Nothing uses this category yet."
                    else "${count.label} will move to $GENERAL_CATEGORY."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    if (expanded.equals(name, ignoreCase = true)) expanded = null
                    viewModel.deleteCategory(name)
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

/**
 * The reorderable part of the list: long-press the grip to drag (same gesture
 * as the quick-add order list), or use the Move chips inside a row's editor.
 */
@Composable
private fun ReorderableCategoryList(
    names: List<String>,
    catalog: CategoryCatalog,
    expanded: String?,
    usageFor: (String) -> CategoryUsage,
    onExpand: (String) -> Unit,
    onReorder: (List<String>) -> Unit,
    onStyle: (String, CategoryStyle) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var displayOrder by remember(names) { mutableStateOf(names) }
    var draggedIndex by remember { mutableStateOf(-1) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 65.dp.toPx() }

    fun moveItem(from: Int, to: Int) {
        if (from == to || from !in displayOrder.indices || to !in displayOrder.indices) return
        displayOrder = displayOrder.toMutableList().apply { add(to, removeAt(from)) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        displayOrder.forEachIndexed { index, name ->
            val isDragged = index == draggedIndex
            CategoryRow(
                name = name,
                subtitle = usageFor(name).label,
                catalog = catalog,
                isExpanded = expanded.equals(name, ignoreCase = true),
                reorderable = true,
                onExpand = { onExpand(name) },
                onStyle = { style -> onStyle(name, style) },
                onRename = { to -> onRename(name, to) },
                onDelete = { onDelete(name) },
                onMoveUp = if (index > 0) {
                    { moveItem(index, index - 1); onReorder(displayOrder) }
                } else null,
                onMoveDown = if (index < displayOrder.lastIndex) {
                    { moveItem(index, index + 1); onReorder(displayOrder) }
                } else null,
                dragHandle = Modifier.pointerInput(index, displayOrder.size) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            draggedIndex = index
                            dragOffsetY = 0f
                        },
                        onDragEnd = {
                            draggedIndex = -1
                            dragOffsetY = 0f
                            onReorder(displayOrder)
                        },
                        onDragCancel = {
                            draggedIndex = -1
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetY += dragAmount.y
                            val from = draggedIndex
                            if (from != -1) {
                                val moves = (dragOffsetY / rowHeightPx).roundToInt()
                                if (moves != 0) {
                                    val to = (from + moves).coerceIn(0, displayOrder.lastIndex)
                                    if (to != from) {
                                        moveItem(from, to)
                                        dragOffsetY -= moves * rowHeightPx
                                        draggedIndex = to
                                    }
                                }
                            }
                        }
                    )
                },
                modifier = Modifier
                    .zIndex(if (isDragged) 1f else 0f)
                    .graphicsLayer { translationY = if (isDragged) dragOffsetY else 0f },
            )
        }
    }
}

/** One category card, collapsed (grip · chip · name · usage · chevron) or expanded with its editor. */
@Composable
private fun CategoryRow(
    name: String,
    subtitle: String,
    catalog: CategoryCatalog,
    isExpanded: Boolean,
    reorderable: Boolean,
    onExpand: () -> Unit,
    onStyle: (CategoryStyle) -> Unit,
    onRename: ((String) -> Unit)?,
    onDelete: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    modifier: Modifier = Modifier,
    dragHandle: Modifier = Modifier,
) {
    val tokens = LocalDashTokens.current
    val style = catalog.styleFor(name)
    val icon = catalog.iconFor(name)
    val swatch = catalog.effectiveSwatch(name)
    var draft by remember(name) { mutableStateOf(name) }

    // A sample chore card so the picked icon and colour can be seen applied the
    // way they will read in the list, matching the Colours screen's preview. The
    // icon and swatch are passed live below, so this only supplies the label/meta.
    val previewChore = remember(name) {
        Chore.from(
            tag = TagDto(
                id = "preview_$name", tagId = "preview_$name", label = "Sample chore",
                category = name, owner = "M", intervalDays = 7.0,
            ),
            lastScanned = Instant.now().minus(Duration.ofDays(2)),
            lastScanId = null,
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                if (isExpanded) Modifier.border(1.5.dp, tokens.pillOutline, MaterialTheme.shapes.medium)
                else Modifier
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = if (isExpanded) "Collapse $name" else "Edit $name"
                }
                .clickable(onClick = onExpand)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (reorderable) {
                Icon(
                    imageVector = LucideIcons.GripVertical,
                    contentDescription = "Drag to reorder $name",
                    tint = tokens.sectionCount,
                    modifier = Modifier
                        .size(28.dp)
                        .then(dragHandle)
                        .padding(6.dp),
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            CardIconChip(
                icon = LucideIcons.forCategory(icon),
                containerColor = swatch.tintColor(),
                contentColor = swatch.textColor(),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (isExpanded && onRename != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(tokens.sheetBlock)
                            .border(1.dp, tokens.pillOutline, MaterialTheme.shapes.small)
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                    ) {
                        BasicTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = "Category name" },
                        )
                    }
                } else {
                    Text(text = name, style = MaterialTheme.typography.titleMedium)
                    MetaCaption(text = subtitle, uppercase = false)
                }
            }
            Icon(
                imageVector = if (isExpanded) LucideIcons.ChevronUp else LucideIcons.ChevronDown,
                contentDescription = null,
                tint = if (isExpanded) MaterialTheme.colorScheme.onSurfaceVariant else tokens.sectionCount,
                modifier = Modifier.size(16.dp),
            )
        }

        if (isExpanded) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Icon", color = tokens.inkFaint)
                    IconPickerRow(
                        selected = icon,
                        swatch = swatch,
                        onSelect = { onStyle(style.copy(icon = it.name)) },
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Colour", color = tokens.inkFaint)
                    SwatchRow(
                        swatches = Swatch.categoryPalette,
                        selected = style.swatchEnum,
                        onSelect = { onStyle(style.copy(swatch = it.name)) },
                        groupLabel = "$name colour",
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionLabel(text = "Preview", color = tokens.inkFaint)
                    ChoreCard(
                        chore = previewChore,
                        showOwner = false,
                        icon = LucideIcons.forCategory(icon),
                        spineSwatch = swatch,
                        iconSwatch = swatch,
                        showCategory = false,
                        inset = 0.dp,
                    )
                }
                if (onMoveUp != null || onMoveDown != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ValueChip(
                            text = "Move up",
                            onClick = { onMoveUp?.invoke() },
                            contentDescription = "Move $name up",
                            chevron = false,
                            content = if (onMoveUp != null) MaterialTheme.colorScheme.onSurface
                                      else MaterialTheme.colorScheme.outline,
                        )
                        ValueChip(
                            text = "Move down",
                            onClick = { onMoveDown?.invoke() },
                            contentDescription = "Move $name down",
                            chevron = false,
                            content = if (onMoveDown != null) MaterialTheme.colorScheme.onSurface
                                      else MaterialTheme.colorScheme.outline,
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (onDelete != null) {
                        val roseText = Color(Swatch.ROSE.tones(isDarkScheme()).textArgb)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(PillShape)
                                .semantics { role = Role.Button }
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 4.dp),
                        ) {
                            Icon(LucideIcons.Trash, contentDescription = null, tint = roseText, modifier = Modifier.size(15.dp))
                            Text(
                                text = "Delete · $subtitle move to $GENERAL_CATEGORY",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                                color = roseText,
                                maxLines = 2,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    } else {
                        Spacer(Modifier.width(1.dp))
                    }
                    ValueChip(
                        text = "Done",
                        onClick = {
                            val newName = draft.trim()
                            if (onRename != null && newName.isNotBlank() && newName != name) onRename(newName)
                            else onExpand()
                        },
                        contentDescription = "Done editing $name",
                        chevron = false,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

/** The Lucide icon picker: 36dp circles, the selected one tinted and ringed. */
@Composable
private fun IconPickerRow(
    selected: CategoryIcon,
    swatch: Swatch,
    onSelect: (CategoryIcon) -> Unit,
) {
    val ring = MaterialTheme.colorScheme.onBackground
    val gap = MaterialTheme.colorScheme.surfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
    ) {
        CategoryIcon.pickerSet.forEach { option ->
            val isSelected = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .semantics {
                        role = Role.RadioButton
                        contentDescription = "Icon: ${option.label}"
                    }
                    .selectable(selected = isSelected, onClick = { onSelect(option) }),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(if (isSelected) 44.dp else 36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) ring else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) gap else Color.Transparent)
                        .padding(if (isSelected) 2.dp else 0.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) swatch.tintColor() else MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Icon(
                        imageVector = LucideIcons.forCategory(option),
                        contentDescription = null,
                        tint = if (isSelected) swatch.textColor() else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }
        }
    }
}
