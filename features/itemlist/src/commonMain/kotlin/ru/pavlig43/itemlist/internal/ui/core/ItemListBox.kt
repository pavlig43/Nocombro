package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.coreui.ScrollBars
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.component.ItemListState
import ru.pavlig43.itemlist.internal.component.StaticItemsBodyComponent
import ru.pavlig43.itemlist.internal.component.StaticItemsBodyComponent1
import ru.pavlig43.itemlist.internal.ui.CHECKBOX_WIDTH


@Composable
internal fun <O : GenericItem, U : IItemUi> ItemListBox(
    listComponent: StaticItemsBodyComponent<O, U>,
    columnDefinition: List<ColumnDefinition<U>>,
    modifier: Modifier = Modifier,

    ) {
    val coroutineScope = rememberCoroutineScope()
    val verticalScrollState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()


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
        StaticItemsListBodyScreen(
            listComponent = listComponent,
            columnDefinition = columnDefinition,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState
        )
        ScrollBars(horizontalScrollState, verticalScrollState)


    }
}

@Composable
internal fun <O : GenericItem, U : IItemUi> ItemListBox1(
    listComponent: StaticItemsBodyComponent1<O, U>,
    modifier: Modifier = Modifier,

    ) {
    val coroutineScope = rememberCoroutineScope()
    val verticalScrollState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()


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
        StaticItemsListBodyScreen1(
            listComponent = listComponent,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,

            )
        ScrollBars(horizontalScrollState, verticalScrollState)


    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun <O : GenericItem, U : IItemUi> StaticItemsListBodyScreen(
    listComponent: StaticItemsBodyComponent<O, U>,
    columnDefinition: List<ColumnDefinition<U>>,
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
    modifier: Modifier = Modifier,
) {

    val itemListState by listComponent.itemListState.collectAsState()

    when (val state = itemListState) {
        is ItemListState.Error -> ErrorScreen(state.message)
        is ItemListState.Initial -> LoadingScreen()
        is ItemListState.Loading -> LoadingScreen()
        is ItemListState.Success<U> -> {

            val checkBoxWidth = CHECKBOX_WIDTH

            val itemList = state.data
            val selectedItemIds = listComponent.selectedItemIds
            val withCheckbox = listComponent.withCheckbox
            val searchText by listComponent.searchText.collectAsState()
            val deleteState by listComponent.deleteState.collectAsState()

            Column(modifier.fillMaxWidth()) {
                if (selectedItemIds.isNotEmpty()) {
                    ActionRow(
                        delete = listComponent::deleteItems,
                        deleteState = deleteState,
                        share = listComponent::shareItems,
                    )
                }

                TableRow(
                    cells = createHeadersCells(
                        withCheckbox = withCheckbox,
                        checkBoxWidth = checkBoxWidth,
                        baseCells = columnDefinition.toBaseCells(),
                    ),
                    scrollState = horizontalScrollState,
                    searchText = "",
                    backgroundColor = MaterialTheme.colorScheme.primary.copy(0.5f),
                    borderColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.height(32.dp)
                )
                LazyColumn(
                    Modifier.fillMaxSize(), state = verticalScrollState
                ) {
                    itemsIndexed(itemList, key = { _, item -> item.id }) { index, item ->
                        Row(
                            Modifier.height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (withCheckbox) {
                                SelectItemCheckBox(
                                    isChecked = item.id in selectedItemIds,
                                    onCheckedChange = { isChecked ->
                                        listComponent.actionInSelectedItemIds(
                                            isChecked,
                                            item.id
                                        )
                                    },
                                    checkboxWidth = checkBoxWidth,
                                )
                            }

                            TableRow(
                                cells = columnDefinition.toCells(item),
                                scrollState = horizontalScrollState,
                                searchText = searchText.value,
                                backgroundColor = MaterialTheme.colorScheme.secondary.copy(
                                    alpha = if (index % 2 == 0) 0.3f else 0.5f
                                ),
                                borderColor = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.height(32.dp)
                                    .clickable { listComponent.onItemClick(item) }
                            )
                        }

                    }
                }
            }
        }
    }

}

private fun createHeadersCells(
    withCheckbox: Boolean = false,
    checkBoxWidth: Int,
    baseCells: List<Cell>
): List<Cell> {

    return if (withCheckbox) {
        listOf(Cell("", checkBoxWidth, textColor = Color.Unspecified)) + baseCells
    } else {
        baseCells
    }
}

private fun createHeadersCells1(
    withCheckbox: Boolean = false,
    checkBoxWidth: Int,
    baseCells: List<TextCellElement>
): List<TextCellElement> {

    return if (withCheckbox) {
        listOf(
            TextCellElement(
                highlightedText = "",
                columnWith = checkBoxWidth,
            )
        ) + baseCells
    } else {
        baseCells
    }
}

private fun <I : IItemUi> List<ColumnDefinition1<I>>.toHeader(): List<TextCellElement> {
    return this.map {
        TextCellElement(
            highlightedText = it.headerTitle,
            columnWith = it.width,
        )
    }
}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun <O : GenericItem, U : IItemUi> StaticItemsListBodyScreen1(
    listComponent: StaticItemsBodyComponent1<O, U>,
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
    modifier: Modifier = Modifier,
) {

    val itemListState by listComponent.itemListState.collectAsState()

    when (val state = itemListState) {
        is ItemListState.Error -> ErrorScreen(state.message)
        is ItemListState.Initial -> LoadingScreen()
        is ItemListState.Loading -> LoadingScreen()
        is ItemListState.Success<U> -> {

            val checkBoxWidth = CHECKBOX_WIDTH

            val itemList = state.data
            val selectedItemIds = listComponent.selectedItemIds
            val withCheckbox = listComponent.withCheckbox
            val searchText by listComponent.searchText.collectAsState()
            val deleteState by listComponent.deleteState.collectAsState()

            Column(modifier.fillMaxWidth()) {
                if (selectedItemIds.isNotEmpty()) {
                    ActionRow(
                        delete = listComponent::deleteItems,
                        deleteState = deleteState,
                        share = listComponent::shareItems,
                    )
                }

                TableRow1<U>(

                    scrollState = horizontalScrollState,
                    searchText = "",
                    generateCells = {
                        createHeadersCells1(
                            withCheckbox = withCheckbox,
                            checkBoxWidth = checkBoxWidth,
                            baseCells = listComponent.columnDefinition.toHeader(),
                        )
                    },
                    item = itemList[0],
                    modifier = Modifier.height(32.dp)
                )
                LazyColumn(
                    Modifier.fillMaxSize(), state = verticalScrollState
                ) {
                    itemsIndexed(itemList, key = { _, item -> item.id }) { index, item ->
                        Row(
                            Modifier.height(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (withCheckbox) {
                                SelectItemCheckBox(
                                    isChecked = item.id in selectedItemIds,
                                    onCheckedChange = { isChecked ->
                                        listComponent.actionInSelectedItemIds(
                                            isChecked,
                                            item.id
                                        )
                                    },
                                    checkboxWidth = checkBoxWidth,
                                )
                            }

                            TableRow1(
                                item = item,

                                scrollState = horizontalScrollState,
                                searchText = searchText.value,
                                modifier = Modifier.height(32.dp)
                                    .clickable { listComponent.onItemClick(item) },
                                generateCells = { listComponent.columnDefinition.toBaseCells(item) },

                                )
                        }

                    }
                }
            }
        }
    }

}



