package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import ru.pavlig43.immutable.internal.ui.HighlightedTableText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readDateTimeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDateTime,
    filterType: TableFilterType.DateTableFilter? = null,
    isSortable: Boolean = true
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readDateTimeCell(valueOf = valueOf)
        if (isSortable){
            sortable()
        }

    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readDateTimeCell(
    valueOf: (T) -> LocalDateTime,
) {
    cell { item, _ ->
        HighlightedTableText(
            text = valueOf(item).toTableDisplayText(),
            modifier = Modifier.padding(horizontal = tableCellHorizontalPadding)
        )
    }
}
