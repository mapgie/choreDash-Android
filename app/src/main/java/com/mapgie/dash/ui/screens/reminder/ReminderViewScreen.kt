package com.mapgie.dash.ui.screens.reminder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import java.time.Instant

// The nudge screen is deliberately dark whatever theme is active (handoff 3a-5):
// it is a moment of quiet, not part of the household's palette. These are the
// screen's own tones and are never fed into MaterialTheme.
private val NudgeGround = Color(0xFF272E24)
private val NudgeText = Color(0xFFF0EAD9)
private val NudgeMuted = Color(0xFF8A9A7C)
private val NudgeGold = Color(0xFFDFCF90)
private val NudgeBellDisc = Color(0xFF323B2E)
private val NudgeSnoozeOutline = Color(0xFF4A5544)
private val NudgeSnoozeText = Color(0xFFC5CDB5)

private val BellDiscSize = 120.dp
private val GlowRingInner = 18.dp
private val GlowRingOuter = 36.dp

/**
 * Full-screen "reminder view" opened by tapping a reminder notification
 * (handoff turn 3a-5). Shows what the nudge was for, how far off its time is,
 * and offers Done or Snooze 1h; the footer names the next pending nudge.
 */
@Composable
fun ReminderViewScreen(
    onBack: () -> Unit,
    viewModel: ReminderViewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.finished) {
        if (uiState.finished) onBack()
    }

    CompositionLocalProvider(LocalContentColor provides NudgeText) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(NudgeGround)
        ) {
            HeaderRow(remindAt = uiState.remindAt)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 34.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                when {
                    uiState.loading -> Unit
                    uiState.missing -> MissingContent(loadFailed = uiState.error != null, onBack = onBack)
                    else -> NudgeContent(
                        subject = uiState.subject,
                        remindAt = uiState.remindAt,
                        onDone = viewModel::markDone,
                        onSnooze = viewModel::snoozeOneHour,
                    )
                }
            }

            if (!uiState.loading && !uiState.missing) {
                Text(
                    text = ReminderViewText.nextLine(uiState.next),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = NudgeMuted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 22.dp, bottom = 34.dp),
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(remindAt: Instant?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, top = 20.dp),
    ) {
        Text(
            text = buildAnnotatedString {
                append("reminder")
                withStyle(SpanStyle(color = NudgeGold)) { append(".") }
            },
            style = MaterialTheme.typography.headlineMedium,
            color = NudgeText,
        )
        Spacer(Modifier.weight(1f))
        if (remindAt != null) {
            Text(
                text = ReminderViewText.headerTime(remindAt),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.08.em,
                ),
                color = NudgeMuted,
            )
        }
    }
}

@Composable
private fun NudgeContent(
    subject: String,
    remindAt: Instant?,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
) {
    BellDisc()
    Spacer(Modifier.height(30.dp))
    Text(
        text = "YOU ASKED FOR A NUDGE",
        style = MaterialTheme.typography.labelMedium.copy(
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.16.em,
        ),
        color = NudgeMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(14.dp))
    Text(
        text = subject,
        style = MaterialTheme.typography.headlineLarge,
        color = NudgeText,
        textAlign = TextAlign.Center,
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
    )
    if (remindAt != null) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = ReminderViewText.metaLine(remindAt, Instant.now()),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = NudgeMuted,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(Modifier.height(44.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DonePill(onClick = onDone)
        SnoozePill(onClick = onSnooze)
    }
}

@Composable
private fun MissingContent(loadFailed: Boolean, onBack: () -> Unit) {
    Text(
        text = "This reminder is gone",
        style = MaterialTheme.typography.headlineLarge,
        color = NudgeText,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = if (loadFailed) "It couldn't be loaded right now." else "It was completed or removed before you got here.",
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        ),
        color = NudgeMuted,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(44.dp))
    OutlinedPill(label = "Back", onClick = onBack)
}

/** 120dp disc with two soft gold glow rings (18dp and 36dp wider) drawn beneath it. */
@Composable
private fun BellDisc() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(BellDiscSize)
            .drawBehind {
                val centre = center
                val radius = size.minDimension / 2f
                drawCircle(NudgeGold.copy(alpha = 0.03f), radius + GlowRingOuter.toPx(), centre)
                drawCircle(NudgeGold.copy(alpha = 0.06f), radius + GlowRingInner.toPx(), centre)
                drawCircle(NudgeBellDisc, radius, centre)
            }
    ) {
        Icon(
            imageVector = LucideIcons.Bell,
            contentDescription = null,
            tint = NudgeGold,
            modifier = Modifier.size(44.dp),
        )
    }
}

@Composable
private fun DonePill(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(PillShape)
            .background(NudgeGold)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 34.dp, vertical = 15.dp),
    ) {
        Text(
            text = "Done ✓",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            ),
            color = NudgeGround,
        )
    }
}

@Composable
private fun SnoozePill(onClick: () -> Unit) {
    OutlinedPill(label = "Snooze 1h", onClick = onClick)
}

@Composable
private fun OutlinedPill(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(PillShape)
            .border(1.5.dp, NudgeSnoozeOutline, PillShape)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 15.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = NudgeSnoozeText,
        )
    }
}
