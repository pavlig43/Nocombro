package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.createButtonNew
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.ReadonlyColumnBuilder

fun<T: IMultiLineTableUi,C,E: TableData<T>> EditableTableColumnsBuilder<T, C, E>.idWithSelection(
    selectionKey:C,
    idKey:C,
    onEvent:(MutableUiEvent)-> Unit,
){
    coreIdWithSelection(
        selectionKey = selectionKey,
        idKey = idKey,
        onCreate = {onEvent(MutableUiEvent.CreateNewItem)},
        onSelectionUiEvent = {selectionUiEvent -> onEvent(MutableUiEvent.Selection(selectionUiEvent))}
    )
}
private fun <T : IMultiLineTableUi, C, E : TableData<T>> EditableTableColumnsBuilder<T, C, E>.coreIdWithSelection(
    selectionKey: C,
    idKey: C,
    onCreate: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit

) {
    column(selectionKey, { it.composeId }) {
        coreSelectionCell(onCreate, onSelectionUiEvent)
    }
    column(idKey, valueOf = { it.composeId }) {
        header("Ид")
        align(Alignment.Center)
        cell { item, _ ->
            Text(
                text = item.composeId.plus(1).toString(),
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
        autoWidth(max = 500.dp)

    }
}
private fun <T : IMultiLineTableUi, C, E : TableData<T>> ReadonlyColumnBuilder<T, C, E>.coreSelectionCell(
    onCreate: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit
) {

    width(48.dp)
    autoWidth(48.dp)
    header {
        createButtonNew {
            onCreate()
        }
    }

    cell { item, tableData ->
        if (tableData.isSelectionMode){
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Checkbox(
                    checked = item.composeId in tableData.selectedIds,
                    onCheckedChange = {
                        onSelectionUiEvent(
                            SelectionUiEvent.ToggleSelection(
                                item.composeId
                            )
                        )
                    },
                )
            }

        }

    }
}
