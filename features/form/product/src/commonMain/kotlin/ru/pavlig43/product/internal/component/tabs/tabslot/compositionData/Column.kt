package ru.pavlig43.product.internal.component.tabs.tabslot.compositionData

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableCellTextFieldNumber
import ru.pavlig43.tablecore.ui.createButtonNew
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

internal enum class CompositionField {
    COMPOSE_ID,
    SELECTION,
    PRODUCT_NAME,

    COUNT
}

internal fun createCompositionColumn(
    onEvent: (MutableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<CompositionUi, CompositionField, TableData<CompositionUi>>> {
    val columns =
        editableTableColumns<CompositionUi, CompositionField, TableData<CompositionUi>> {

            column(CompositionField.SELECTION, valueOf = { it.composeId }) {
                title {
                    createButtonNew {
                        onEvent(
                            MutableUiEvent.CreateNewItem
                        )
                    }
                }
                autoWidth(48.dp)
                cell { item, tableData ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Checkbox(
                            checked = item.composeId in tableData.selectedIds,
                            onCheckedChange = {
                                onEvent(
                                    MutableUiEvent.Selection(
                                        SelectionUiEvent.ToggleSelection(
                                            item.composeId
                                        )
                                    )
                                )
                            },
                        )
                    }
                }
            }
            column(CompositionField.PRODUCT_NAME, { it.productName }) {
                header("Название")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item.productName) }

                sortable()
            }

            column(CompositionField.COUNT, { it.count }) {
                header("Количество")
                align(Alignment.Center)
                filter(TableFilterType.TextTableFilter())
                cell { item, _ -> Text(item.count.toString()) }
                editCell { item: CompositionUi, tableData: TableData<CompositionUi>, onComplete: () -> Unit ->

                    TableCellTextFieldNumber(
                        value = item.count,
                        saveInModel = {
                            onEvent(MutableUiEvent.UpdateItem(item.copy(count = it)))
                        },
                        countDigitsAfterDot = 3,
                        onComplete = onComplete,
                    )
                }
                sortable()
            }


        }
    return columns

}
