package ru.pavlig43.tablecore.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder


fun <T : ITableUi, C, E : TableData<T>> ReadonlyTableColumnsBuilder<T, C, E>.coreIdWithSelection(
    selectionKey: C,
    idKey: C,
    onCreate: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit,


) {

    column(selectionKey, { it.composeId }) {
        coreSelectionCell(onCreate, onSelectionUiEvent)
    }
    column(idKey, valueOf = { it.composeId }) {
        header("Ид")
        align(Alignment.Center)
        cell { item, _ -> Text(item.composeId.toString()) }
        autoWidth(max = 500.dp)

    }
}

fun <T : ITableUi, C, E : TableData<T>> EditableTableColumnsBuilder<T, C, E>.coreIdWithSelection(
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
        cell { item, _ -> Text(item.composeId.plus(1).toString()) }
        autoWidth(max = 500.dp)

    }
}

private fun <T : ITableUi, C, E : TableData<T>> ReadonlyColumnBuilder<T, C, E>.coreSelectionCell(
    onCreate: () -> Unit,
    onSelectionUiEvent: (SelectionUiEvent) -> Unit
) {
    title {
        createButtonNew {
            onCreate()
        }
    }
    autoWidth(48.dp)
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
