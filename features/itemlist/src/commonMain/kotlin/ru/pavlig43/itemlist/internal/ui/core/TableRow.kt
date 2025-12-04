package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Suppress("LongParameterList")
@Composable
internal fun TableRow(
    cells: List<Cell>,
    searchText: String,
    scrollState: ScrollState,
    backgroundColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),


        ) {
        cells.forEach { cell->
            TableCell(
                cell = cell,
                searchText = searchText,
                backgroundColor = backgroundColor,
                borderColor = borderColor

            )
        }
    }
}