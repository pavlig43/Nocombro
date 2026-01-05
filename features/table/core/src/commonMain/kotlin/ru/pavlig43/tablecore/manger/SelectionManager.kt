package ru.pavlig43.tablecore.manger

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SelectionManager(
    componentContext: ComponentContext,

    ) : ComponentContext by componentContext {

    private val _selectedIds = MutableStateFlow(emptySet<Int>())
    val selectedIdsFlow = _selectedIds.asStateFlow()

    val selectedIds: Set<Int>
        get() = _selectedIds.value


    fun toggleSelection(id: Int) {
        _selectedIds.update { selectedIds ->
            if (id in selectedIds) {
                selectedIds - id
            } else {
                selectedIds + id
            }
        }

    }

    fun toggleSelectAll(displayedIds: Set<Int>) {
        val newSelectedIds = if (displayedIds.all { it in _selectedIds.value }) {
            _selectedIds.value - displayedIds
        } else {
            _selectedIds.value + displayedIds
        }
        _selectedIds.update { newSelectedIds }
    }

    fun clearSelected() {
        _selectedIds.update { emptySet() }
    }
    fun onEvent(event: SelectionUiEvent) {
        when (event) {
            is SelectionUiEvent.ClearSelection -> clearSelected()


            is SelectionUiEvent.ToggleSelectAll -> {
                toggleSelectAll(
                    displayedIds = event.displayedIds.toSet()
                )
            }

            is SelectionUiEvent.ToggleSelection -> {
                toggleSelection(event.id)
            }
        }
    }

}
sealed class SelectionUiEvent {


    /** Toggle selection for a  item by ID */
    data class ToggleSelection(
        val id: Int,
    ) : SelectionUiEvent()

    /** Toggle selection for all displayed items */
    data class ToggleSelectAll(val displayedIds: Set<Int>) : SelectionUiEvent()

    /** Delete all selected items */
//    data object DeleteSelected : SelectionUiEvent()

    /** Clear all selections */
    data object ClearSelection : SelectionUiEvent()
}