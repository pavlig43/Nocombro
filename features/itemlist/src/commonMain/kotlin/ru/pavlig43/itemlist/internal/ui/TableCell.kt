package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun TableCell(
    cell: Cell,
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
        Text(cell.title)
    }
}

internal data class Cell(
    val title: String,
    val columnWith: Int
)