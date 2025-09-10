package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.component.DeleteState
import ru.pavlig43.itemlist.api.data.ItemUi
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow

@Suppress("MagicNumber","LongParameterList")
@Composable
internal fun ItemListBody(
    itemList: List<ItemUi>,
    withCheckbox: Boolean,
    selectedItemIds: List<Int>,
    actionInSelectedItemIds: (Boolean, Int) -> Unit,
    deleteState: DeleteState,
    onCreate: () -> Unit,
    deleteItems: (List<Int>) -> Unit,
    shareItems: (List<Int>) -> Unit,
    fullListSelection: List<ItemType>,
    saveSelection: (List<ItemType>) -> Unit,
    horizontalScrollState: ScrollState,
    verticalScrollState: LazyListState,
    onClickItem:(id:Int,name:String)->Unit,
    searchText:String,
    onSearchChange:(String)->Unit,
    modifier: Modifier = Modifier,
) {

    Column(modifier.fillMaxSize()) {
        SettingsRow(
            onCreate = onCreate,
            fullListSelection = fullListSelection,
            saveSelection = saveSelection,
            searchText = searchText,
            onSearchChange = onSearchChange,
        )
        if (selectedItemIds.isNotEmpty()) {
            ActionRow(
                delete = { deleteItems(selectedItemIds) },
                share = { shareItems(selectedItemIds) },
                deleteState = deleteState,
                modifier = Modifier.height(28.dp)
            )
        }
        TableRow(
            cells = createHeadersCells(withCheckbox),
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
                    if (withCheckbox){
                        SelectItemCheckBox(
                            isChecked = item.id in selectedItemIds, onCheckedChange = { isChecked ->
                                actionInSelectedItemIds(isChecked, item.id)
                            }
                        )
                    }

                    TableRow(
                        cells = item.toCell(),
                        scrollState = horizontalScrollState,
                        searchText = searchText,
                        backgroundColor = MaterialTheme.colorScheme.secondary.copy(
                            alpha = if (index % 2 == 0) 0.3f else 0.5f
                        ),
                        borderColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.height(32.dp).clickable { onClickItem(item.id,item.displayName) }
                    )
                }

            }
        }
    }

}


private fun ItemUi.toCell(): List<Cell> = listOf(
    Cell(id.toString(), ID_WIDTH),
    Cell(displayName, NAME_WIDTH),
    Cell(type.displayName, TYPE_WIDTH),
    Cell(createdAt, CREATED_AT_WIDTH),
    Cell(comment, COMMENT_WIDTH)
)
private fun createHeadersCells(withCheckbox: Boolean = false): List<Cell> {
    val baseCells = listOf(
        Cell(ID, ID_WIDTH),
        Cell(NAME, NAME_WIDTH),
        Cell(TYPE, TYPE_WIDTH),
        Cell(CREATED_AT, CREATED_AT_WIDTH),
        Cell(COMMENT, COMMENT_WIDTH),
    )

    return if (withCheckbox) {
        listOf(Cell("", CHECKBOX_WIDTH)) + baseCells
    } else {
        baseCells
    }
}





