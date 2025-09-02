package ru.pavlig43.itemlist.api.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.component.DeleteState
import ru.pavlig43.itemlist.api.component.IItemListComponent
import ru.pavlig43.itemlist.api.component.ItemListState
import ru.pavlig43.itemlist.api.data.ItemUi
import ru.pavlig43.itemlist.internal.ui.ItemListBody
import ru.pavlig43.itemlist.internal.ui.ItemListBox


@Composable
fun ItemListScreen(
    component: IItemListComponent,
    modifier: Modifier = Modifier,
) {
    val itemListState by component.itemListState.collectAsState()
    val deleteState by component.deleteState.collectAsState()
    val selectedItemIds = component.selectedItemIds

    ItemListScreenState(
        state = itemListState,
        onCreate = { component.onCreate() },
        actionInSelectedItemIds = component::actionInSelectedItemIds,
        selectedItemIds = selectedItemIds,
        fullListSelection = component.fullListSelection,
        saveSelection = component::saveSelection,
        deleteItems = component::deleteItems,
        shareItems = component::shareItems,
        deleteState = deleteState,
        onItemClick = {ind,name ->component.onItemClick(ind,name)},
        withCheckbox = component.withCheckbox,
        modifier = modifier)
}

@Suppress("LongParameterList")
@Composable
private fun ItemListScreenState(
    state: ItemListState,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    onCreate: () -> Unit,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,
    onItemClick: (Int,String) -> Unit,
    withCheckbox: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ItemListState.Error -> ErrorScreen(state.message,modifier)
        is ItemListState.Initial -> LoadingScreen(modifier)
        is ItemListState.Loading -> LoadingScreen(modifier)
        is ItemListState.Success -> ItemList(
            itemList = state.data,
            selectedItemIds = selectedItemIds,
            actionInSelectedItemIds = actionInSelectedItemIds,
            onCreate = onCreate,
            fullListSelection = fullListSelection,
            saveSelection = saveSelection,
            deleteItems = deleteItems,
            shareItems = shareItems,
            deleteState = deleteState,
            onItemClick = onItemClick,
            withCheckbox = withCheckbox,
            modifier = modifier
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ItemList(
    itemList: List<ItemUi>,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    onCreate: () -> Unit,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,
    onItemClick:(Int,String)->Unit,
    withCheckbox: Boolean,
    modifier: Modifier = Modifier
) {
    val verticalScrollState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()


    ItemListBox(
        verticalScrollState = verticalScrollState,
        horizontalScrollState = horizontalScrollState,
        modifier = modifier
    ) {
        ItemListBody(
            itemList = itemList,
            selectedItemIds =selectedItemIds,
            actionInSelectedItemIds = actionInSelectedItemIds,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,
            onCreate = onCreate,
            fullListSelection = fullListSelection,
            saveSelection = saveSelection,
            deleteItems = deleteItems,
            shareItems = shareItems,
            deleteState = deleteState,
            onClickItem = onItemClick,
            withCheckbox = withCheckbox
        )
    }


}





