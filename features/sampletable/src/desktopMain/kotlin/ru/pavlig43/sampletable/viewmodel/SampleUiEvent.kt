package ru.pavlig43.sampletable.viewmodel

import ru.pavlig43.sampletable.model.MegaType
import ru.pavlig43.sampletable.model.Person
import ru.pavlig43.sampletable.model.Position

/** UI events for sample app, including table editing events. */
sealed class SampleUiEvent {
    /** Start editing a row - creates a mutable copy */
    data class StartEditing(
        val rowIndex: Int,
        val person: Person,
    ) : SampleUiEvent()

    /** Update name field during editing */
    data class UpdateName(
        val name: String,
    ) : SampleUiEvent()

    /** Update age field during editing */
    data class UpdateAge(
        val age: Int,
    ) : SampleUiEvent()

    /** Update email field during editing */
    data class UpdateEmail(
        val email: String,
    ) : SampleUiEvent()

    /** Update position field during editing */
    data class UpdatePosition(
        val position: Position,
    ) : SampleUiEvent()

    /** Update megaType field during editing */
    data class UpdateMegaType(
        val megaType: MegaType,
    ) : SampleUiEvent()

    /** Update salary field during editing */
    data class UpdateSalary(
        val salary: Int,
    ) : SampleUiEvent()

    /** Complete editing - saves changes to the people list */
    data object CompleteEditing : SampleUiEvent()

    /** Cancel editing - discards changes */
    data object CancelEditing : SampleUiEvent()

    /** Toggle selection for a person by ID */
    data class ToggleSelection(
        val personId: Int,
    ) : SampleUiEvent()

    /** Toggle selection for all displayed persons */
    data object ToggleSelectAll : SampleUiEvent()

    /** Delete all selected persons */
    data object DeleteSelected : SampleUiEvent()

    /** Clear all selections */
    data object ClearSelection : SampleUiEvent()
}
