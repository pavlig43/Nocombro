package ru.pavlig43.itemlist.core.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.seanproctor.datatable.DataTableState
import com.seanproctor.datatable.rememberDataTableState
import kotlinx.coroutines.launch
import ru.pavlig43.itemlist.core.ui.ScrollBars

@Composable
fun ItemListBox(
    modifier: Modifier = Modifier,
    itemList: @Composable (tableState: DataTableState) -> Unit

) {
    val coroutineScope = rememberCoroutineScope()
    val tableState: DataTableState = rememberDataTableState()

    val horizontallyDraggableModifier = Modifier.Companion.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            coroutineScope.launch {
                tableState.horizontalScrollState.scrollBy(-delta)
            }
        }
    )
    val verticalDraggableModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            coroutineScope.launch {
                tableState.verticalScrollState.scrollBy(-delta)
            }
        }
    )
    Box(
        modifier
            .fillMaxSize()
            .then(horizontallyDraggableModifier)
            .then(verticalDraggableModifier)
    ) {
        itemList(
            tableState
        )

        ScrollBars(tableState)


    }
}