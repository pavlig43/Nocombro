package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.dateTimeFormat
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readDateTimeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDateTime,
    filterType: TableFilterType.DateTableFilter? = null
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readDateTimeCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readDateTimeCell(
    valueOf: (T) -> LocalDateTime,
) {
    cell { item, _ ->
        Text(
            text = valueOf(item).format(dateTimeFormat),
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
