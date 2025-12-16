package ru.pavlig43.itemlist.statik.internal.ui.refactor

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