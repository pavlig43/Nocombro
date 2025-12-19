package ru.pavlig43.itemlist.core.refac.core.component.manager

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.wwind.table.filter.data.TableFilterState

internal class FilterManager<C>(
    componentContext: ComponentContext
): ComponentContext by componentContext {
    private val _filters = MutableStateFlow<Map<C, TableFilterState<*>>>(emptyMap())
    val filters: StateFlow<Map<C, TableFilterState<*>>> = _filters.asStateFlow()

    fun update(filters: Map<C, TableFilterState<*>>) {
        _filters.value = filters
    }
}
