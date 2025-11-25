package ru.pavlig43.coreui.itemlist

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.SlotComponent

@Suppress("MagicNumber", "LongParameterList")
@Composable
fun AnyItemListBody(
    itemList: List<IItemUi>,
    withCheckbox: Boolean,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
    onClickItem: (IItemUi) -> Unit,
    searchText: String,
    settingRow: @Composable () -> Unit,
    baseCells: List<Cell>,
    checkBoxWidth: Int,
    mainCellFactory: (IItemUi) -> List<Cell>,
    modifier: Modifier = Modifier,
) {

    Column(modifier.fillMaxSize()) {
        settingRow()
        if (selectedItemIds.isNotEmpty()) {
            ActionRow(
                delete = { deleteItems(selectedItemIds) },
                share = { shareItems(selectedItemIds) },
                deleteState = deleteState,
                modifier = Modifier.height(28.dp)
            )
        }
        TableRow(
            cells = createHeadersCells(
                withCheckbox = withCheckbox,
                checkBoxWidth = checkBoxWidth,
                baseCells = baseCells,
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
                Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (withCheckbox) {
                        SelectItemCheckBox(
                            isChecked = item.id in selectedItemIds,
                            onCheckedChange = { isChecked ->
                                actionInSelectedItemIds(isChecked, item.id)
                            },
                            checkboxWidth = checkBoxWidth,
                        )
                    }

                    TableRow(
                        cells = mainCellFactory(item),
                        scrollState = horizontalScrollState,
                        searchText = searchText,
                        backgroundColor = MaterialTheme.colorScheme.secondary.copy(
                            alpha = if (index % 2 == 0) 0.3f else 0.5f
                        ),
                        borderColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.height(32.dp)
                            .clickable { onClickItem(item) }
                    )
                }

            }
        }
    }

}



interface IItemUi {
    val id: Int
    val displayName: String
}


fun createHeadersCells(
    withCheckbox: Boolean = false,
    checkBoxWidth: Int,
    baseCells: List<Cell>
): List<Cell> {

    return if (withCheckbox) {
        listOf(Cell("", checkBoxWidth)) + baseCells
    } else {
        baseCells
    }
}