package ru.pavlig43.immutable.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.tablecore.ui.SearchHighlightedText

internal fun findSearchMatches(
    text: String,
    query: String,
): List<IntRange> = ru.pavlig43.tablecore.ui.findSearchMatches(text, query)

@Composable
internal fun HighlightedTableText(
    text: String,
    modifier: Modifier = Modifier,
) {
    SearchHighlightedText(
        text = text,
        query = LocalImmutableTableUiContext.current.searchQuery,
        modifier = modifier,
    )
}
