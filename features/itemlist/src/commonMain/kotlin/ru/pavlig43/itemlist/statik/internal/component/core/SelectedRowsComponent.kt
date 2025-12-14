package ru.pavlig43.itemlist.statik.internal.component.core

import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext

internal class SelectedRowsComponent(
    componentContext: ComponentContext
): ComponentContext by componentContext{
    private val _selectedItemIds = mutableStateListOf<Int>()
    val selectedItemIds: List<Int>
        get() = _selectedItemIds
    fun clearSelectedIds() {
        _selectedItemIds.clear()
    }
    fun actionInSelectedItemIds( id: Int,checked: Boolean,) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }
}