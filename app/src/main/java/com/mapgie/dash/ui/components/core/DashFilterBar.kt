package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The filter bar scaffold shared by the Chores and Tasks lists: selection [chips] on
 * the left, trailing icon [actions] on the right, with one consistent inset and
 * spacing. Each screen fills the slots with its own chips and toggles, so the layout
 * stays identical while the controls stay screen-specific.
 */
@Composable
fun DashFilterBar(
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    chips: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        chips()
        Spacer(Modifier.weight(1f))
        actions()
    }
}
