package com.mapgie.dash.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.data.model.AddMenuOption
import com.mapgie.dash.data.preferences.DEFAULT_FAB_ORDER
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LocalTypeAccents
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.TypeAccentColors

private data class AddMenuOptionSpec(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
)

private fun AddMenuOption.spec(reminderLabel: String, accents: TypeAccentColors): AddMenuOptionSpec = when (this) {
    AddMenuOption.REMINDER -> AddMenuOptionSpec(
        LucideIcons.Bell, "New $reminderLabel", accents.reminderContainer, accents.onReminderContainer
    )
    AddMenuOption.CHORE -> AddMenuOptionSpec(
        LucideIcons.HouseCheck, "New chore", accents.choreContainer, accents.onChoreContainer
    )
    AddMenuOption.TASK -> AddMenuOptionSpec(
        LucideIcons.CircleCheck, "New task", accents.taskContainer, accents.onTaskContainer
    )
}

/**
 * The round sage add button docked in the centre slot of the bottom bar (52dp).
 * While the speed dial is open the fill flips to ink and the plus rotates 45°
 * into a cross; the same button is drawn again inside [SpeedDialOverlay] so it
 * sits above the scrim.
 */
@Composable
fun AddMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(targetValue = if (expanded) 45f else 0f, label = "fabRotation")
    FloatingActionButton(
        onClick = { onExpandedChange(!expanded) },
        shape = CircleShape,
        containerColor = if (expanded) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.secondary,
        contentColor = if (expanded) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSecondary,
        modifier = modifier
            .size(52.dp)
            .semantics { contentDescription = if (expanded) "Close add menu" else "Add" },
    ) {
        Icon(
            imageVector = LucideIcons.Plus,
            contentDescription = null,
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer { rotationZ = rotation },
        )
    }
}

/**
 * The "+" speed dial (handoff 7a): a scrim over the whole screen, nav bar
 * included, with three pills rising above the add button (New chore · New task
 * · New memo). The active tab's item sits lowest, closest to the thumb; the
 * rest follow the order chosen in Settings › Quick add button. Scrim tap, Back,
 * or the cross closes it. Lives outside the Scaffold so it can cover the bar.
 */
@Composable
fun SpeedDialOverlay(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelect: (AddMenuOption) -> Unit,
    order: List<AddMenuOption> = DEFAULT_FAB_ORDER,
    activeOption: AddMenuOption? = null,
    reminderLabel: String = "Reminder",
    modifier: Modifier = Modifier,
) {
    val accents = LocalTypeAccents.current
    val tokens = LocalDashTokens.current
    BackHandler(enabled = expanded) { onExpandedChange(false) }

    // Bottom to top: the active tab's item first, then the configured order.
    val bottomUp = remember(order, activeOption) {
        val rest = order.filter { it != activeOption }
        if (activeOption != null && activeOption in order) listOf(activeOption) + rest else rest
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.scrim)
                    .semantics {
                        role = Role.Button
                        contentDescription = "Close add menu"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { onExpandedChange(false) },
                    )
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp),
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                    exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        bottomUp.asReversed().forEach { option ->
                            val spec = option.spec(reminderLabel, accents)
                            SpeedDialPill(spec = spec, onClick = {
                                onExpandedChange(false)
                                onSelect(option)
                            })
                        }
                    }
                }
                AddMenuButton(expanded = true, onExpandedChange = onExpandedChange)
            }
        }
    }
}

@Composable
private fun SpeedDialPill(spec: AddMenuOptionSpec, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .shadow(elevation = 8.dp, shape = PillShape)
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .heightIn(min = 56.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(spec.containerColor),
        ) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = spec.contentColor,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = spec.label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}
