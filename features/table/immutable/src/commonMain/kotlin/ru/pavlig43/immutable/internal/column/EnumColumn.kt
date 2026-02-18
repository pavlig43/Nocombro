package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E, ENUM : Enum<ENUM>> ReadonlyTableColumnsBuilder<T, C, E>.readEnumColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> ENUM,
    filterType: TableFilterType.EnumTableFilter<ENUM>? = null,
    getTitle: (ENUM) -> String = { it.toString() }
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readEnumCell(valueOf = valueOf, getTitle = getTitle)
        sortable()
    }
}

private fun <T : Any, C, E, ENUM : Enum<ENUM>> ReadonlyColumnBuilder<T, C, E>.readEnumCell(
    valueOf: (T) -> ENUM,
    getTitle: (ENUM) -> String
) {
    cell { item, _ ->
        Text(
            text = getTitle(valueOf(item)),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
