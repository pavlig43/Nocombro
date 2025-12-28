package ru.pavlig43.immutable.internal.component.items.composition

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal data class CompositionUi(
    override val composeId: Int,
    val productId: Int,
    val productName: String,
    val count: Double,
) : ITableUi

internal enum class CompositionField {
    COMPOSE_ID,
    SELECTION,
    PRODUCT_NAME,

    COUNT
}

internal fun createCompositionColumn(
    onEvent: (CompositionUiEvent) -> Unit,
) {
    val columns =
        editableTableColumns<CompositionUi, CompositionField, TableData<CompositionUi>> {

            column(CompositionField.SELECTION, valueOf = { it.composeId }) {
                title { "" }
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
                                    CompositionUiEvent.Selection(
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
                editCell { item: CompositionUi, tableData: TableData<CompositionUi>, onComplete: () -> Unit ->
                    TableCellTextFieldWithTooltipError(
                        value = item.count.toString(),
                        onValueChange = {
                            onEvent(CompositionUiEvent.UpdateCount(item.composeId,it.toDouble()))
                        },
                        errorMessage = "",
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = { onComplete() },
                            ),
                    )
                }
//                cell { item, _ -> Text(item.count.toString()) }
                sortable()
            }


        }

}



