package ru.pavlig43.immutable.internal.component

import ru.pavlig43.tablecore.manger.SelectionUiEvent


internal interface MutableUiEvent{
    data class Selection(val selectionUiEvent: SelectionUiEvent): MutableUiEvent
    data object DeleteSelected: MutableUiEvent
}