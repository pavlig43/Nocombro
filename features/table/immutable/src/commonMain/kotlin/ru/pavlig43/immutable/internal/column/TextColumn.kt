package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readTextColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> String,
    filterType: TableFilterType.TextTableFilter? = null
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readTextCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readTextCell(
    valueOf: (T) -> String,
) {
    cell { item, _ ->
        Text(
            text = valueOf(item),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
