package ru.pavlig43.mutable.api.column

import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.coreIdWithSelection
import ua.wwind.table.EditableTableColumnsBuilder

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
