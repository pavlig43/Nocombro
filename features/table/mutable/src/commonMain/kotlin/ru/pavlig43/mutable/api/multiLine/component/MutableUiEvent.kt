package ru.pavlig43.mutable.api.multiLine.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi

sealed interface MutableUiEvent{
    data class Selection(val selectionUiEvent: SelectionUiEvent): MutableUiEvent
    data object DeleteSelected: MutableUiEvent

    data object CreateNewItem: MutableUiEvent

    data class UpdateItem(val item: IMultiLineTableUi): MutableUiEvent

}
