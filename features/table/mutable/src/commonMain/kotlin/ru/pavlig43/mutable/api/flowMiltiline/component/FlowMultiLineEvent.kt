package ru.pavlig43.mutable.api.flowMiltiline.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent

sealed interface FlowMultiLineEvent {
    data class Selection(val selectionUiEvent: SelectionUiEvent) : FlowMultiLineEvent
    data object DeleteSelected : FlowMultiLineEvent
    data class CallChoiceDialog(val id: Int): FlowMultiLineEvent
}