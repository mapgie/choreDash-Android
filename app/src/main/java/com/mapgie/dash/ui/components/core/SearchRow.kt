package com.mapgie.dash.ui.components.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.mapgie.dash.ui.theme.PillShape

/**
 * Cozy Cream search row (handoff 3a-2): a pill text field with a leading search
 * glyph and a trailing Cancel action. Focus lands in the field as soon as the
 * row appears.
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
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholder) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = PillShape,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
        )
        TextButton(onClick = onCancel) { Text("Cancel") }
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
        background = MaterialTheme.colorScheme.tertiaryContainer,
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
