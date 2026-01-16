package ru.pavlig43.coreui.tab

import androidx.compose.animation.core.Animatable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

/**
 * Описывает строку в которой находятся вкладки.
 * Вкладки можно менять местами
 */
@Suppress("LongParameterList")
@Composable
fun <T : Any> TabDraggableRow(
    items: List<T>,
    onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    tabRowPadding: PaddingValues = PaddingValues(2.dp),
    tabHorizontalSpacing: Arrangement.Horizontal = Arrangement.spacedBy(2.dp),
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

private inline fun <T : Any> LazyListScope.itemsIndexed(
    items: List<T>,
    controller: DragController,
    crossinline itemContent: @Composable (index: Int, item: T, draggedItem: T?, modifier: Modifier) -> Unit,
) {
    itemsIndexed(
        items = items,
        contentType = { index, _ -> DraggableMetadata(index) }) { index, item ->
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
private fun rememberDragController(
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
    var countItems by remember { mutableStateOf(totalDraggableItems) }
    LaunchedEffect(controller) {
        for (scrollDelta in controller.scrollChannel) {
            lazyListState.scrollBy(scrollDelta)
        }
    }

    LaunchedEffect(totalDraggableItems) {
        if (totalDraggableItems > countItems){
            lazyListState.animateScrollToItem(totalDraggableItems - 1)
            countItems = totalDraggableItems
        }
    }
    return controller
}


/**
 * Контроллер перетаскивания элементов в LazyList с поддержкой:
 * - Определения элемента под касанием
 * - Автоматической перестановки при пересечении с соседними элементами
 * - Автоскролла при достижении границ viewport
 * - Плавной анимации смещения и возврата
 *
 * Используется совместно с [DraggableMetadata] в contentType элементов списка.
 *
 * @property totalItems Общее количество элементов для проверки границ скролла
 * @property listState Состояние LazyList для отслеживания видимых элементов
 * @property onReorder Callback `(fromIndex: Int, toIndex: Int) -> Unit` для уведомления о перестановке
 */
private class DragController(
    private val totalItems: Int,
    private val listState: LazyListState,
    private val onReorder: (Int, Int) -> Unit
) {
    /** Индекс текущего перетаскиваемого элемента (nullable для состояния "не перетаскиваем") */
    var draggedItemIndex: Int? by mutableStateOf(null)

    /** Анимированное смещение перетаскиваемого элемента по горизонтали */
    var offset = Animatable(0f)

    /** Канал для асинхронной отправки команд автоскролла краев списка */
    val scrollChannel = Channel<Float>()

    /** Информация о текущем перетаскиваемом элементе LazyList (кеш для оптимизации) */
    private var draggedItemInfo: LazyListItemInfo? = null

    /**
     * Инициация перетаскивания: определяет элемент под касанием и сохраняет его индекс
     * @param offset Координаты касания относительно начала списка
     */
    fun beginDrag(offset: Offset) {
        draggedItemInfo = listState.layoutInfo.visibleItemsInfo
            // Находит первый видимый элемент под координатами касания
            .firstOrNull { offset.x.toInt() in it.offset until it.offset + it.size }
            // Проверяет, что элемент поддерживает перетаскивание
            ?.takeIf { it.contentType is DraggableMetadata }
            // Сохраняет индекс элемента из метаданных
            ?.also { draggedItemIndex = (it.contentType as DraggableMetadata).index }
    }

    /**
     * Отмена перетаскивания с анимацией возврата в исходное положение
     */
    suspend fun cancelDrag() {
        offset.animateTo(0f) // Плавно возвращает элемент на место
        clearDragState()
    }

    /**
     * Обновляет позицию перетаскивания и проверяет необходимость перестановки/скролла
     * @param offsetDelta Изменение позиции относительно предыдущего кадра
     */
    suspend fun updateDragPosition(offsetDelta: Offset) {
        // Обновляет горизонтальное смещение элемента
        offset.snapTo(offset.value + offsetDelta.x)
        val currentIndex = draggedItemIndex ?: return
        val currentItem = draggedItemInfo ?: return

        // Вычисляет текущие границы перетаскиваемого элемента с учетом смещения
        val startPosition = currentItem.offset + offset.value
        val endPosition = startPosition + currentItem.size
        val midpoint = startPosition + (endPosition - startPosition) / 2

        // Проверяет пересечение с целевым элементом по середине перетаскиваемого
        val targetItem = listState.layoutInfo.visibleItemsInfo
            .firstOrNull {
                // Центр перетаскиваемого элемента над целевым
                midpoint.toInt() in it.offset until it.offset + it.size &&
                        // Не тот же элемент
                        currentItem.index != it.index &&
                        // Целевой элемент поддерживает перетаскивание
                        it.contentType is DraggableMetadata
            }

        if (targetItem != null) {
            // Есть цель для перестановки
            reorderItems(currentIndex, targetItem)
        } else {
            // Нет цели - проверяем необходимость автоскролла
            handleScroll(startPosition, endPosition, currentIndex)
        }
    }

    /**
     * Выполняет перестановку элементов и корректирует смещение для плавности
     */
    private suspend fun reorderItems(currentIndex: Int, targetItem: LazyListItemInfo) {
        val targetIndex = (targetItem.contentType as DraggableMetadata).index
        onReorder(currentIndex, targetIndex) // Уведомляет родителя о смене порядка

        // Обновляет состояние перетаскиваемого элемента
        draggedItemIndex = targetIndex
        // Корректирует смещение для визуальной плавности перехода
        offset.snapTo(offset.value + draggedItemInfo!!.offset - targetItem.offset)
        draggedItemInfo = targetItem
        println(scrollChannel.consumeEach {a-> println(a)  })
    }

    /**
     * Вычисляет необходимость автоскролла при достижении границ viewport
     */
    private fun handleScroll(startPosition: Float, endPosition: Float, currentIndex: Int) {
        val toViewportStart = startPosition - listState.layoutInfo.viewportStartOffset
        val toViewportEnd = endPosition - listState.layoutInfo.viewportEndOffset

        /** Дельта скролла: отрицательная для начала, положительная для конца viewport */
        val scrollDelta = when {
            toViewportStart < 0 -> toViewportStart
            toViewportEnd > 0 -> toViewportEnd
            else -> 0f
        }

        // Отправляет команду скролла только для внутренних элементов (не граничных)
        if (scrollDelta != 0f && currentIndex !in listOf(0, totalItems - 1)) {
            scrollChannel.trySend(scrollDelta)
        }
    }

    /** Очищает состояние перетаскивания */
    private fun clearDragState() {
        draggedItemInfo = null
        draggedItemIndex = null
    }
}


private data class DraggableMetadata(val index: Int)