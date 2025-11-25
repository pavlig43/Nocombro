package ru.pavlig43.itemlist.api.ui

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.coreui.itemlist.AnyItemListBody
import ru.pavlig43.coreui.itemlist.Cell
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.coreui.itemlist.ItemListBox
import ru.pavlig43.itemlist.api.component.IItemListComponent
import ru.pavlig43.itemlist.api.component.ItemListState
import ru.pavlig43.itemlist.api.data.ItemUi
import ru.pavlig43.itemlist.internal.ui.CHECKBOX_WIDTH
import ru.pavlig43.itemlist.internal.ui.COMMENT
import ru.pavlig43.itemlist.internal.ui.COMMENT_WIDTH
import ru.pavlig43.itemlist.internal.ui.CREATED_AT
import ru.pavlig43.itemlist.internal.ui.CREATED_AT_WIDTH
import ru.pavlig43.itemlist.internal.ui.ID
import ru.pavlig43.itemlist.internal.ui.ID_WIDTH
import ru.pavlig43.itemlist.internal.ui.NAME
import ru.pavlig43.itemlist.internal.ui.NAME_WIDTH
import ru.pavlig43.itemlist.internal.ui.TYPE
import ru.pavlig43.itemlist.internal.ui.TYPE_WIDTH
import ru.pavlig43.itemlist.internal.ui.settings.DefaultSettingsRow


@Composable
fun ItemListScreen(
    component: IItemListComponent,
    modifier: Modifier = Modifier,
) {
    val itemListState by component.itemListState.collectAsState()
    val deleteState by component.deleteState.collectAsState()
    val selectedItemIds = component.selectedItemIds
    val searchText by component.searchField.collectAsState()

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
        onItemClick = {  component.onItemClick(it) },
        searchText = searchText,
        onSearchChange = component::onSearchChange,
        withCheckbox = component.withCheckbox,
        modifier = modifier
    )
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
    onItemClick: (IItemUi) -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
    withCheckbox: Boolean,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ItemListState.Error -> ErrorScreen(state.message, modifier)
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
            searchText = searchText,
            onSearchChange = onSearchChange,
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
    onItemClick: (IItemUi) -> Unit,
    searchText: String,
    onSearchChange: (String) -> Unit,
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
        AnyItemListBody(
            itemList = itemList,
            withCheckbox = withCheckbox,
            selectedItemIds = selectedItemIds,
            actionInSelectedItemIds = actionInSelectedItemIds,
            deleteState = deleteState,
            deleteItems = deleteItems,
            shareItems = shareItems,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,
            onClickItem = onItemClick,
            searchText = searchText,
            settingRow = {
                DefaultSettingsRow(
                    onCreate = onCreate,
                    fullListSelection = fullListSelection,
                    saveSelection = saveSelection,
                    searchText = searchText,
                    onSearchChange = onSearchChange,
                )
            },
            baseCells = baseCells,
            checkBoxWidth = CHECKBOX_WIDTH,
            mainCellFactory = {(it as ItemUi).toCell()},
        )

    }

}

private val baseCells = listOf(
    Cell(ID, ID_WIDTH),
    Cell(NAME, NAME_WIDTH),
    Cell(TYPE, TYPE_WIDTH),
    Cell(CREATED_AT, CREATED_AT_WIDTH),
    Cell(COMMENT, COMMENT_WIDTH),
)
private fun ItemUi.toCell(): List<Cell> = listOf(
    Cell(id.toString(), ID_WIDTH),
    Cell(displayName, NAME_WIDTH),
    Cell(type.displayName, TYPE_WIDTH),
    Cell(createdAt, CREATED_AT_WIDTH),
    Cell(comment, COMMENT_WIDTH)
)





