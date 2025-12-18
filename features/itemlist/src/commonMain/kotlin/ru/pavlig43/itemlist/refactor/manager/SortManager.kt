package ru.pavlig43.itemlist.refactor.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.wwind.table.state.SortState

class SortManager<C>(
    componentContext: ComponentContext
): ComponentContext by componentContext {
    private val _sort = MutableStateFlow<SortState<C>?>(null)
    val sort: StateFlow<SortState<C>?> = _sort.asStateFlow()

    fun update(sort: SortState<C>?) {
        _sort.value = sort
    }
}