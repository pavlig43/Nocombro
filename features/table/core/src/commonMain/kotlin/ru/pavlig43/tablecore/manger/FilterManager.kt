package ru.pavlig43.tablecore.manger

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ua.wwind.table.filter.data.TableFilterState


class FilterManager<Column>(
    componentContext: ComponentContext
): ComponentContext by componentContext {
    private val _filters = MutableStateFlow<Map<Column, TableFilterState<*>>>(emptyMap())
    val filters: StateFlow<Map<Column, TableFilterState<*>>> = _filters.asStateFlow()

    fun update(filters: Map<Column, TableFilterState<*>>) {
        _filters.value = filters
    }
}