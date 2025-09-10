package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TableCell(
    cell: Cell,
    searchText: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    borderColor: Color

) {
    Box(
        modifier.width(cell.columnWith.dp)
            .fillMaxHeight()
            .background(backgroundColor)
            .border(Dp.Hairline, borderColor),
        contentAlignment = Alignment.Center
    ) {
        HighlightedText(
            text = cell.title,
            searchText = searchText
        )
        Text(cell.title)
    }
}
@Composable
private fun HighlightedText(
    text: String,
    searchText: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = remember(text, searchText) {
        buildAnnotatedString {
            append(text)

            if (searchText.isNotBlank()) {
                var startIndex = 0
                while (true) {
                    val foundIndex = text.indexOf(searchText, startIndex, ignoreCase = true)
                    if (foundIndex == -1) break

                    addStyle(
                        style = SpanStyle(
                            background = Color.Yellow.copy(alpha = 0.7f),
                            textDecoration = TextDecoration.Underline,
                            color = Color.Red
                        ),
                        start = foundIndex,
                        end = foundIndex + searchText.length
                    )

                    startIndex = foundIndex + searchText.length
                }
            }
        }
    }

    Text(text = annotatedString, modifier = modifier)
}

internal data class Cell(
    val title: String,
    val columnWith: Int
)
