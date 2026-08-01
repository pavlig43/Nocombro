package ru.pavlig43.immutable.internal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

internal fun findSearchMatches(
    text: String,
    query: String,
): List<IntRange> {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return emptyList()

    return buildList {
        var cursor = 0
        while (cursor < text.length) {
            val start = text.indexOf(
                string = normalizedQuery,
                startIndex = cursor,
                ignoreCase = true,
            )
            if (start < 0) break
            add(start until start + normalizedQuery.length)
            cursor = start + normalizedQuery.length
        }
    }
}

@Composable
internal fun HighlightedTableText(
    text: String,
    modifier: Modifier = Modifier,
) {
    val matches = findSearchMatches(
        text = text,
        query = LocalImmutableTableUiContext.current.searchQuery,
    )
    if (matches.isEmpty()) {
        Text(text = text, modifier = modifier)
        return
    }

    val highlightStyle = SpanStyle(
        background = MaterialTheme.colorScheme.primaryContainer,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    val annotatedText = buildAnnotatedString {
        var cursor = 0
        matches.forEach { match ->
            append(text.substring(cursor, match.first))
            withStyle(highlightStyle) {
                append(text.substring(match.first, match.last + 1))
            }
            cursor = match.last + 1
        }
        append(text.substring(cursor))
    }
    Text(text = annotatedText, modifier = modifier)
}
