package ru.pavlig43.coreui.itemlist

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun ItemListBox(
    verticalScrollState: LazyListState,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()



    val horizontallyDraggableModifier = Modifier.draggable(
        orientation = Orientation.Horizontal,
        state = rememberDraggableState { delta ->
            coroutineScope.launch {
                horizontalScrollState.scrollBy(-delta)
            }
        }
    )
    val verticalDraggableModifier = Modifier.draggable(
        orientation = Orientation.Vertical,
        state = rememberDraggableState { delta ->
            coroutineScope.launch {
                verticalScrollState.scrollBy(-delta)
            }
        }
    )
    Box(
        modifier
            .fillMaxSize()
            .then(horizontallyDraggableModifier)
            .then(verticalDraggableModifier)
    ) {
        content()
        ScrollBars(horizontalScrollState, verticalScrollState)


    }
}
@Composable
internal expect fun BoxScope.ScrollBars(
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState
)

