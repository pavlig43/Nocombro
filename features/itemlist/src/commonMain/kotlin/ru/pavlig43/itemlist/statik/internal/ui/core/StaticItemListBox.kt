package ru.pavlig43.itemlist.statik.internal.ui.core

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.seanproctor.datatable.DataColumn
import com.seanproctor.datatable.DataTableState
import com.seanproctor.datatable.TableRowScope
import com.seanproctor.datatable.material3.DataTable
import com.seanproctor.datatable.paging.rememberPaginatedDataTableState
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.core.ui.ItemListBox
import ru.pavlig43.itemlist.core.ui.rowContentWithOptionalCheckbox
import ru.pavlig43.itemlist.core.ui.withOptionalCheckboxHeader
import ru.pavlig43.itemlist.statik.internal.component.ItemListState
import ru.pavlig43.itemlist.statik.internal.component.StaticListComponent

@Composable
internal fun <O : GenericItem, U : IItemUi> StaticItemListBox(
    listComponent: StaticListComponent<O, U>,
    headers: List<DataColumn>,
    contentRow: TableRowScope.(U) -> Unit,
) {
    ItemListBox { tableState ->
        StaticItemsListBodyScreen(
            listComponent = listComponent,
            headers = headers,
            contentRow = contentRow,
            tableState = tableState,
        )
    }

}

@Suppress("LongMethod", "MagicNumber")
@Composable
private fun <U : IItemUi> StaticItemsListBodyScreen(
    listComponent: StaticListComponent<out GenericItem, U>,
    headers: List<DataColumn>,
    contentRow: TableRowScope.(U) -> Unit,
    tableState: DataTableState,
    modifier: Modifier = Modifier,
) {

    val itemListState by listComponent.itemListState.collectAsState()
    val headerColumnsWithCheckBox = headers.withOptionalCheckboxHeader(listComponent.withCheckbox)
    val rowContentWithCheckbox = rowContentWithOptionalCheckbox(
        withCheckbox = listComponent.withCheckbox,
        selectedRowsComponent = listComponent.selectedRowsComponent,
        baseContent = contentRow
    )


    when (val state = itemListState) {
        is ItemListState.Error<*> -> ErrorScreen(state.message)
        is ItemListState.Initial -> LoadingScreen()
        is ItemListState.Loading -> LoadingScreen()
        is ItemListState.Success<U> -> {


            val itemList = state.data
            val selectedRowsComponent = listComponent.selectedRowsComponent
            val deleteState by listComponent.deleteState.collectAsState()


            Column(modifier.fillMaxWidth()) {
                StaticSettingsRow(
                    onCreate = listComponent.onCreate,
                    searchTextComponent = listComponent.searchTextComponent
                )
                if (selectedRowsComponent.selectedItemIds.isNotEmpty()) {
                    StaticActionRow(
                        delete = listComponent::deleteItems,
                        deleteState = deleteState,
                        share = listComponent::shareItems,
                    )
                }
                val evenBackgroundColor = MaterialTheme.colorScheme.secondary.copy( 0.3f)
                val oddBackgroundColor = MaterialTheme.colorScheme.secondary.copy( 0.5f)

                DataTable(
                    state = tableState,
                    columns = headerColumnsWithCheckBox,
                    headerBackgroundColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = modifier
                ) {
                    itemList.forEach {
                        row {
                            backgroundColor = if (it.id % 2 == 0) evenBackgroundColor else oddBackgroundColor
                            onClick = { listComponent.onItemClick(it) }
                            rowContentWithCheckbox(it)
                        }
                    }


                }


            }
        }
    }

}