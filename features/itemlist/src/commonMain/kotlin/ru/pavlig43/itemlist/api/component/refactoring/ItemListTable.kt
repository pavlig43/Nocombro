package ru.pavlig43.itemlist.api.component.refactoring

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.coreui.itemlist.Cell
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.coreui.itemlist.SelectItemCheckBox
import ru.pavlig43.coreui.itemlist.TableRow
import ru.pavlig43.coreui.itemlist.createHeadersCells
import ru.pavlig43.itemlist.api.component.ItemListState1

internal const val CHECKBOX_WIDTH = 48
internal const val ID_WIDTH = 40
internal const val NAME_WIDTH = 500
internal const val TYPE_WIDTH = 300
internal const val CREATED_AT_WIDTH = 300
internal const val COMMENT_WIDTH = 400

internal const val ID = "ИД"
internal const val NAME = "Название"
private const val TYPE_DOCUMENT = "Тип Документа"
internal const val CREATED_AT = "Время создания"
internal const val COMMENT = "Комментарий"


@Composable
fun GeneralItemListScreen(
    component: ItemListFactoryComponent,
    modifier: Modifier = Modifier
){
    when(val listComponent = component.listComponent){
        is DeclarationListComponent -> Box(modifier)
        is DocumentsListComponent -> DocumentListScreen(listComponent)
    }

}
private val columnDefinition = listOf<ColumnDefinition<DocumentItemUi>>(
    ColumnDefinition(
        title = ID,
        width = ID_WIDTH,
        valueProvider = {it.id.toString()}
    ),
    ColumnDefinition(
        title = NAME,
        width = NAME_WIDTH,
        valueProvider = {it.displayName}
    ),
    ColumnDefinition(
        title = TYPE_DOCUMENT,
        width = TYPE_WIDTH,
        valueProvider = {it.type.displayName}
    ),
    ColumnDefinition(
        title = CREATED_AT,
        width = CREATED_AT_WIDTH,
        valueProvider = {it.createdAt.convertToDateTime()}
    ),
    ColumnDefinition(
        title = COMMENT,
        width = COMMENT_WIDTH,
        valueProvider = {it.comment}
    )
)
@Composable
internal fun DocumentListScreen(
    component: DocumentsListComponent,
    modifier: Modifier= Modifier
){

    ItemsListBodyScreen(
        listComponent = component.itemsBodyComponent,
        columnDefinition = columnDefinition,
    )
}


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
                                cells = columnDefinition.toCells(),
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


data class ColumnDefinition<O : IItemUi>(
    val title: String,
    val width: Int,
    val valueProvider: (O) -> String
)


private fun <I : IItemUi> List<ColumnDefinition<I>>.toBaseCells() = this.map { Cell(it.title, it.width) }

private fun <I : IItemUi> List<ColumnDefinition<I>>.toCells() = this.map {
    val provider = it.valueProvider
    @Suppress("UNCHECKED_CAST")
    (Cell(provider(this as I), it.width))
}