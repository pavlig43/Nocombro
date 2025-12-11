package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.HighlightedText
import ru.pavlig43.coreui.HighlightedTextField
import ru.pavlig43.itemlist.api.data.IItemUi

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
    }
}

internal data class Cell(
    val title: String,
    val columnWith: Int,
    val textColor: Color
)

sealed interface CellElement {
    val columnWith: Int
}

data class TextCellElement(
    val highlightedText: String,
    override val columnWith: Int,

    ) : CellElement

data class TextFieldCellElement(
    val highlightedText: String,
    override val columnWith: Int,
    val onValueChange: (String) -> Unit,

    ) : CellElement

@Composable
internal fun<I: IItemUi> TableRow1(
    item:I,
    generateCells:I.( )->List<CellElement>,
    searchText: String,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val cells  = remember {  item.generateCells()}
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .background(
                MaterialTheme.colorScheme.secondary.copy(
                    alpha = if (item.composeKey % 2 == 0) 0.3f else 0.5f
                )
            )

    ) {
        cells.forEach { cell ->
            TableCell1(
                cellElement = cell,
                searchText = searchText,
            )
        }
    }
}

@Composable
internal fun TableCell1(
    cellElement: CellElement,
    searchText: String,
    modifier: Modifier = Modifier

) {
    Box(
        modifier
            .width(cellElement.columnWith.dp)
            .fillMaxHeight()
            .border(width = Dp.Hairline, color = MaterialTheme.colorScheme.onSecondary),
        contentAlignment = Alignment.Center
    ) {

        CellUi(
            cellElement = cellElement,
            searchText = searchText,
            modifier = Modifier.fillMaxWidth()
        )

    }
}

@Composable
fun CellUi(
    cellElement: CellElement,
    searchText: String,
    modifier: Modifier = Modifier
) {
    when (cellElement) {
        is TextCellElement -> HighlightedText(
            text = cellElement.highlightedText,
            searchText = searchText,
            modifier = modifier
        )

        is TextFieldCellElement -> HighlightedTextField(
             value = cellElement.highlightedText,
            onValueChange = cellElement.onValueChange,
            searchText = searchText,
            modifier = modifier

        )
    }

}