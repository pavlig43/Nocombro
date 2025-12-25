package ru.pavlig43.itemlist.internal.utils

import ru.pavlig43.itemlist.api.model.IItemUi
import ua.wwind.table.state.SortState

interface SortMatcher<I, C> {
    fun sort(items: List<I>, sort: SortState<C>?): List<I>
}