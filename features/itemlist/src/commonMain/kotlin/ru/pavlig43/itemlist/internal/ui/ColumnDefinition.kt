package ru.pavlig43.itemlist.internal.ui

import ru.pavlig43.coreui.itemlist.Cell
import ru.pavlig43.coreui.itemlist.IItemUi

internal data class ColumnDefinition<O : IItemUi>(
    val title: String,
    val width: Int,
    val valueProvider: (O) -> String
)
internal fun <I : IItemUi> List<ColumnDefinition<I>>.toBaseCells() = this.map { Cell(it.title, it.width) }

internal fun <I : IItemUi> List<ColumnDefinition<I>>.toCells(item: I): List<Cell> = this.map {
    Cell(it.valueProvider(item), it.width)
}