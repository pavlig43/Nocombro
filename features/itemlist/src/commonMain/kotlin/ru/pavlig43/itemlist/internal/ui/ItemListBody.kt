package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.api.component.DeleteState
import ru.pavlig43.itemlist.api.data.ItemUi
import ru.pavlig43.itemlist.internal.ui.settings.SettingsRow

@Suppress("MagicNumber")
@Composable
internal fun ItemListBody(
    itemList: List<ItemUi>,
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
    modifier: Modifier = Modifier,
) {

    require(itemList.all { it.type in fullListSelection }) {
        val invalid = itemList.filter { it.type !in fullListSelection }.map { it.type }.toSet()
        "Недопустимые типы: $invalid"
    }



    Column(modifier.fillMaxSize()) {
        SettingsRow(
            onCreate = onCreate,
            fullListSelection = fullListSelection,
            saveSelection = saveSelection
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
            cells = headersCells,
            scrollState = horizontalScrollState,
            backgroundColor = MaterialTheme.colorScheme.primary.copy(0.5f),
            borderColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.height(32.dp)
        )
        LazyColumn(
            Modifier.fillMaxSize(), state = verticalScrollState
        ) {
            itemsIndexed(itemList, key = { _, item -> item.id }) { index, item ->
                Row(Modifier.height(32.dp), verticalAlignment = Alignment.CenterVertically) {
                    SelectItemCheckBox(
                        isChecked = item.id in selectedItemIds, onCheckedChange = { isChecked ->
                            actionInSelectedItemIds(isChecked, item.id)
                        }
                    )
                    TableRow(
                        cells = item.toCell(),
                        scrollState = horizontalScrollState,
                        backgroundColor = MaterialTheme.colorScheme.secondary.copy(
                            alpha = if (index % 2 == 0) 0.3f else 0.5f
                        ),
                        borderColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.height(32.dp)
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
)

private val headersCells = listOf(
    Cell("", CHECKBOX_WIDTH),
    Cell(ID, ID_WIDTH),
    Cell(NAME, NAME_WIDTH),
    Cell(TYPE, TYPE_WIDTH),
    Cell(CREATED_AT, CREATED_AT_WIDTH),

    )





