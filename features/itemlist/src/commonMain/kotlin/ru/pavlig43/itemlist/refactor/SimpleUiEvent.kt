package ru.pavlig43.itemlist.refactor

/** UI events for sample app, including table editing events. */
sealed class SampleUiEvent {


    /** Toggle selection for a person by ID */
    data class ToggleSelection(
        val documentId: Int,
    ) : SampleUiEvent()

    /** Toggle selection for all displayed persons */
    data object ToggleSelectAll : SampleUiEvent()

    /** Delete all selected persons */
    data object DeleteSelected : SampleUiEvent()

    /** Clear all selections */
    data object ClearSelection : SampleUiEvent()
}
sealed class SelectionUiEvent {


    /** Toggle selection for a person by ID */
    data class ToggleSelection(
        val id: Int,
    ) : SelectionUiEvent()

    /** Toggle selection for all displayed persons */
    data object ToggleSelectAll : SelectionUiEvent()

    /** Delete all selected persons */
    data object DeleteSelected : SelectionUiEvent()

    /** Clear all selections */
    data object ClearSelection : SelectionUiEvent()
}