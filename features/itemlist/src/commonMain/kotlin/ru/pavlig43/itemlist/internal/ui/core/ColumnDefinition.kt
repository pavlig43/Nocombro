package ru.pavlig43.itemlist.internal.ui.core

import androidx.compose.ui.graphics.Color
import ru.pavlig43.itemlist.api.data.IItemUi

internal data class ColumnDefinition<O : IItemUi>(
    val title: String,
    val titleColor: Color = Color.Unspecified,
    val width: Int,
    val valueProvider: (O) -> String,
    val colorProvider: (O) -> Color = { Color.Unspecified },
)
internal fun <I : IItemUi> List<ColumnDefinition<I>>.toBaseCells() = this.map { Cell(it.title, it.width,it.titleColor) }

internal fun <I : IItemUi> List<ColumnDefinition<I>>.toCells(item: I): List<Cell> = this.map {
    Cell(it.valueProvider(item), it.width,it.colorProvider(item))
}
