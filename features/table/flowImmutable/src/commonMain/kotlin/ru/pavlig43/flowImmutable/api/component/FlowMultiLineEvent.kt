package ru.pavlig43.flowImmutable.api.component

import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.tablecore.manger.SelectionUiEvent

sealed interface FlowMultiLineEvent {

    data class Selection(val selectionUiEvent: SelectionUiEvent) : FlowMultiLineEvent
    data object DeleteSelected : FlowMultiLineEvent
}