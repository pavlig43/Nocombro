package ru.pavlig43.itemlist.core.refac.core.utils

import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ua.wwind.table.state.SortState

interface SortMatcher<I : IItemUi, C> {
    fun sort(items: List<I>, sort: SortState<C>?): List<I>
}