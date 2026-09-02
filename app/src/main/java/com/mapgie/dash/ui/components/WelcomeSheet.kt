package com.mapgie.dash.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.components.core.SectionLabel
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.PillShape

/**
 * The first-run welcome sheet: the chores / tasks / memos distinction, told
 * once (handoff 7a says the speed dial carries no hint text because this and
 * Settings › Help do the teaching). Dismissing it in any way marks it seen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeSheet(
    reminderLabel: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tokens = LocalDashTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = tokens.scrim,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel(text = "Welcome")
            Text(
                "Three kinds of thing.",
                style = MaterialTheme.typography.headlineMedium,
            )
            HelpContent(reminderLabel = reminderLabel, showTips = false)
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = onDismiss,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp),
            ) {
                Text("Got it", fontWeight = FontWeight.ExtraBold)
            }
            Text(
                "You can read this again under Settings › Help.",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = tokens.inkFaint,
            )
        }
    }
}
