package ru.pavlig43.itemlist.internal.component.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal class SelectionManager(
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

}