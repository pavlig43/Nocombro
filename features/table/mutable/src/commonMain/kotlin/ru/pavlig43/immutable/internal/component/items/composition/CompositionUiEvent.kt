package ru.pavlig43.immutable.internal.component.items.composition

import ru.pavlig43.immutable.internal.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent

internal sealed interface CompositionUiEvent: MutableUiEvent {
    data class UpdateCount(val composeId: Int,val count: Double): CompositionUiEvent
    data class Selection(val selectionUiEvent: SelectionUiEvent): CompositionUiEvent
    data object DeleteSelected: CompositionUiEvent
}