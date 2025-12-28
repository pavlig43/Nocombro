package ru.pavlig43.tablecore.utils

import ua.wwind.table.state.SortState

interface SortMatcher<I, C> {
    fun sort(items: List<I>, sort: SortState<C>?): List<I>
}