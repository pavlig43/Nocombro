package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Properties
 */
private val defaultTabSpacing = 2.dp
private val defaultTabPadding = 2.dp

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun <T : Any> TabDraggableRow(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    tabRowPadding: PaddingValues = PaddingValues(defaultTabPadding),
    tabHorizontalSpacing: Arrangement.Horizontal = Arrangement.spacedBy(defaultTabSpacing),
    itemContent: @Composable (index: Int, item: T, isDragging: Boolean, modifier: Modifier) -> Unit
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val dragDropState = rememberDragController(
        lazyListState = lazyListState,
        totalDraggableItems = items.size,
        onItemReordered = { fromIndex, toIndex ->
            onMove(fromIndex, toIndex)
        }
    )

    LazyRow(
        modifier = modifier.pointerInput(dragDropState) {
            detectDragGestures(
                onDrag = { change, offset ->
                    change.consume()
                    coroutineScope.launch { dragDropState.updateDragPosition(offset) }
                },
                onDragStart = { offset -> dragDropState.beginDrag(offset) },
                onDragEnd = { coroutineScope.launch { dragDropState.cancelDrag() } },
                onDragCancel = { coroutineScope.launch { dragDropState.cancelDrag() } }
            )
        },
        state = lazyListState,
        contentPadding = tabRowPadding,
        horizontalArrangement = tabHorizontalSpacing
    ) {
        itemsIndexed(items, dragDropState) { index, item, holding, modifier ->
            itemContent(index, item, holding == item, modifier)
        }
    }
}

inline fun <T : Any> LazyListScope.itemsIndexed(
    items: List<T>,
    controller: DragController,
    crossinline itemContent: @Composable (index: Int, item: T, draggedItem: T?, modifier: Modifier) -> Unit,
) {
    itemsIndexed(items = items, contentType = { index, _ -> DraggableMetadata(index) }) { index, item ->
        val isDragged = controller.draggedItemIndex == index
        val modifier = Modifier
            .then(
                if (isDragged) Modifier
                    .zIndex(1f)
                    .graphicsLayer { translationX = controller.offset.value }
                else Modifier
            )
        itemContent(index, item, controller.draggedItemIndex?.let { items[it] }, modifier)
    }
}

@Composable
fun rememberDragController(
    lazyListState: LazyListState,
    totalDraggableItems: Int,
    onItemReordered: (oldIndex: Int, newIndex: Int) -> Unit
): DragController {
    val controller = remember(lazyListState) {
        DragController(
            totalItems = totalDraggableItems,
            listState = lazyListState,
            onReorder = onItemReordered
        )
    }
    LaunchedEffect(controller) {
        for (scrollDelta in controller.scrollChannel) {
            lazyListState.scrollBy(scrollDelta)
        }
    }
    return controller
}

class DragController(
    private val totalItems: Int,
    private val listState: LazyListState,
    private val onReorder: (Int, Int) -> Unit
) {
    var draggedItemIndex: Int? by mutableStateOf(null)
    var offset = Animatable(0f)
    val scrollChannel = Channel<Float>()

    private var draggedItemInfo: LazyListItemInfo? = null

    internal fun beginDrag(offset: Offset) {
        draggedItemInfo = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { offset.x.toInt() in it.offset until it.offset + it.size }
            ?.takeIf { it.contentType is DraggableMetadata }
            ?.also { draggedItemIndex = (it.contentType as DraggableMetadata).index }
    }

    internal suspend fun cancelDrag() {
        offset.animateTo(0f)
        clearDragState()
    }

    internal suspend fun updateDragPosition(offsetDelta: Offset) {
        offset.snapTo(offset.value + offsetDelta.x)
        val currentIndex = draggedItemIndex ?: return
        val currentItem = draggedItemInfo ?: return

        val startPosition = currentItem.offset + offset.value
        val endPosition = startPosition + currentItem.size
        val midpoint = startPosition + (endPosition - startPosition) / 2

        val targetItem = listState.layoutInfo.visibleItemsInfo
            .firstOrNull {
                midpoint.toInt() in it.offset until it.offset + it.size &&
                        currentItem.index != it.index &&
                        it.contentType is DraggableMetadata
            }

        if (targetItem != null) {
            reorderItems(currentIndex, targetItem)
        } else {
            handleScroll(startPosition, endPosition, currentIndex)
        }
    }

    private suspend fun reorderItems(currentIndex: Int, targetItem: LazyListItemInfo) {
        val targetIndex = (targetItem.contentType as DraggableMetadata).index
        onReorder(currentIndex, targetIndex)
        draggedItemIndex = targetIndex
        offset.snapTo(offset.value + draggedItemInfo!!.offset - targetItem.offset)
        draggedItemInfo = targetItem
    }

    private fun handleScroll(startPosition: Float, endPosition: Float, currentIndex: Int) {
        val toViewportStart = startPosition - listState.layoutInfo.viewportStartOffset
        val toViewportEnd = endPosition - listState.layoutInfo.viewportEndOffset

        val scrollDelta = when {
            toViewportStart < 0 -> toViewportStart
            toViewportEnd > 0 -> toViewportEnd
            else -> 0f
        }

        if (scrollDelta != 0f && currentIndex !in listOf(0, totalItems - 1)) {
            scrollChannel.trySend(scrollDelta)
        }
    }

    private fun clearDragState() {
        draggedItemInfo = null
        draggedItemIndex = null
    }
}

data class DraggableMetadata(val index: Int)