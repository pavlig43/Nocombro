package ru.pavlig43.immutable.internal.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.ITableUi


internal sealed interface ImmutableTableUiEvent{
    data class Selection(val selectionUiEvent: SelectionUiEvent) : ImmutableTableUiEvent
    data object DeleteSelected : ImmutableTableUiEvent
    data object CreateNewItem: ImmutableTableUiEvent

}
