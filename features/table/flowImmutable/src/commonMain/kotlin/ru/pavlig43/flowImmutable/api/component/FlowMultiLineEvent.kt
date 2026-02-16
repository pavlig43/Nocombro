package ru.pavlig43.flowImmutable.api.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi

sealed interface FlowMultiLineEvent {
    data class RowClick<I: IMultiLineTableUi>(val item: I) : FlowMultiLineEvent
    data class Selection(val selectionUiEvent: SelectionUiEvent) : FlowMultiLineEvent
    data object DeleteSelected : FlowMultiLineEvent
}