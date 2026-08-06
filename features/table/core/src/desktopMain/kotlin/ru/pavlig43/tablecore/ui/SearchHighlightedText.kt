package ru.pavlig43.tablecore.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle

/**
 * Возвращает диапазоны всех непересекающихся совпадений поискового запроса.
 *
 * Сравнение не зависит от регистра, а пробелы по краям запроса игнорируются.
 */
fun findSearchMatches(
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

/**
 * Показывает текст и выделяет части, совпавшие с поисковым запросом.
 *
 * Это общий рендерер подсветки для таблиц продуктов и склада.
 */
@Composable
fun SearchHighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
) {
    val matches = findSearchMatches(text = text, query = query)
    if (matches.isEmpty()) {
        Text(text = text, modifier = modifier, textAlign = textAlign)
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
    Text(text = annotatedText, modifier = modifier, textAlign = textAlign)
}
