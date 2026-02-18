package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readDateColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDate,
    filterType: TableFilterType.DateTableFilter? = null
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readDateCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readDateCell(
    valueOf: (T) -> LocalDate,
) {
    cell { item, _ ->
        Text(
            text = valueOf(item).format(dateFormat),
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}
