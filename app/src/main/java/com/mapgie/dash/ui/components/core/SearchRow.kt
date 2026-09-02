package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mapgie.dash.ui.theme.LocalDashTokens
import com.mapgie.dash.ui.theme.LucideIcons
import com.mapgie.dash.ui.theme.PillShape
import com.mapgie.dash.ui.theme.isDarkScheme

/**
 * Cozy Cream search row (handoff 3a-2): a card-coloured pill with a leading
 * search glyph, a sage caret, and a sage "Cancel" text action beside it. Focus
 * lands in the field as soon as the row appears.
 */
@Composable
fun SearchRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onCancel: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val tokens = LocalDashTokens.current
    val textStyle = LocalTextStyle.current.copy(
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 6.dp, bottom = 4.dp)
    ) {
        Surface(
            shape = PillShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = if (isDarkScheme()) 0.dp else 2.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 46.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Icon(
                    imageVector = LucideIcons.Search,
                    contentDescription = null,
                    tint = tokens.inkFaint,
                    modifier = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            placeholder,
                            style = textStyle,
                            color = tokens.inkFaint,
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = textStyle,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.secondary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { /* results update live */ }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .semantics { contentDescription = placeholder },
                    )
                }
            }
        }
        TextButton(onClick = onCancel) {
            Text(
                "Cancel",
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.5.sp, fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

/**
 * [text] with every case-insensitive occurrence of [query] emphasised, for
 * highlighting the matching substring in search results (handoff 3a-2).
 */
@Composable
fun highlightedText(text: String, query: String?): AnnotatedString {
    if (query.isNullOrBlank()) return AnnotatedString(text)
    val style = SpanStyle(
        background = MaterialTheme.colorScheme.secondaryContainer,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
        fontWeight = FontWeight.ExtraBold,
    )
    return buildAnnotatedString {
        var from = 0
        while (from < text.length) {
            val idx = text.indexOf(query, from, ignoreCase = true)
            if (idx < 0) {
                append(text.substring(from))
                break
            }
            append(text.substring(from, idx))
            withStyle(style) { append(text.substring(idx, idx + query.length)) }
            from = idx + query.length
        }
    }
}
