package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.coreui.itemlist.SelectItemCheckBox
import ru.pavlig43.coreui.itemlist.TableRow
import ru.pavlig43.coreui.itemlist.createHeadersCells
import ru.pavlig43.itemlist.api.component.ItemListState1
import ru.pavlig43.itemlist.internal.component.ItemsBodyComponent

@Composable
internal fun <O : GenericItem, U : IItemUi> ItemsListBodyScreen(
    listComponent: ItemsBodyComponent<O, U>,
    columnDefinition: List<ColumnDefinition<U>>,
    modifier: Modifier = Modifier,
) {

    val itemListState by listComponent.itemListState.collectAsState()

    when (val state = itemListState) {
        is ItemListState1.Error -> ErrorScreen(state.message)
        is ItemListState1.Initial -> LoadingScreen()
        is ItemListState1.Loading -> LoadingScreen()
        is ItemListState1.Success<U> -> {

            val checkBoxWidth = CHECKBOX_WIDTH
            val verticalScrollState = rememberLazyListState()
            val horizontalScrollState = rememberScrollState()
            val itemList = state.data
            val selectedItemIds = listComponent.selectedItemIds
            val withCheckbox = listComponent.withCheckbox
            val searchText by listComponent.searchText.collectAsState()

            Column(modifier.fillMaxWidth()) {
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
                    modifier = Modifier.Companion.height(32.dp)
                )
                LazyColumn(
                    Modifier.Companion.fillMaxSize(), state = verticalScrollState
                ) {
                    itemsIndexed(itemList, key = { _, item -> item.id }) { index, item ->
                        Row(
                            Modifier.Companion.height(32.dp),
                            verticalAlignment = Alignment.Companion.CenterVertically
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
                                modifier = Modifier.Companion.height(32.dp)
                                    .clickable { listComponent.onItemClick(item) }
                            )
                        }

                    }
                }
            }
        }
    }


}


