package ru.pavlig43.itemlist.core.refac.core.component

sealed class SelectionUiEvent {


    /** Toggle selection for a  item by ID */
    data class ToggleSelection(
        val id: Int,
    ) : SelectionUiEvent()

    /** Toggle selection for all displayed items */
    data object ToggleSelectAll : SelectionUiEvent()

    /** Delete all selected items */
    data object DeleteSelected : SelectionUiEvent()

    /** Clear all selections */
    data object ClearSelection : SelectionUiEvent()
}