package ru.pavlig43.immutable.internal.ui

import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.coreIdWithSelection
import ua.wwind.table.ReadonlyTableColumnsBuilder

internal fun<T: ITableUi,C,E: TableData<T>> ReadonlyTableColumnsBuilder<T, C, E>.idWithSelection(
    selectionKey:C,
    idKey:C,
    onEvent:(ImmutableTableUiEvent)-> Unit,
){
    coreIdWithSelection(
        selectionKey = selectionKey,
        idKey = idKey,
        onCreate = {onEvent(ImmutableTableUiEvent.CreateNewItem)},
        onSelectionUiEvent = {selectionUiEvent -> onEvent(ImmutableTableUiEvent.Selection(selectionUiEvent))}
    )
}
