package ru.pavlig43.tablecore.manger

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import ua.wwind.table.state.SortState

class SortManager<Column>(
    componentContext: ComponentContext
): ComponentContext by componentContext {
    private val _sort = MutableStateFlow<SortState<Column>?>(null)
    val sort: StateFlow<SortState<Column>?> = _sort.asStateFlow()

    fun update(sort: SortState<Column>?) {
        _sort.update { sort }
    }
}