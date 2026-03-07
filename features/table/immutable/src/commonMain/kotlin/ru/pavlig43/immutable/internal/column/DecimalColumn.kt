package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.coreui.toStartDoubleFormat
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType



@Suppress("LongParameterList")
fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readDecimalColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Int,
    decimalFormat: DecimalFormat,
    alignment: Alignment = Alignment.Center,
    textModifier: Modifier = Modifier.padding(horizontal = 12.dp),
    filterType: TableFilterType.NumberTableFilter<Int>? = null,
    isSortable: Boolean = true
) {
    column(column, valueOf = { valueOf(it) }) {
        autoWidth(300.dp)
        header(headerText)
        filterType?.let {
            filter(it)
        }
        align(alignment)
        readDecimalCell(format = decimalFormat, getCount = valueOf,modifier = textModifier)
        if (isSortable){
            sortable()
        }
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readDecimalCell(
    format: DecimalFormat,
    getCount: (T) -> Int,
    modifier: Modifier
) {
    cell { item, _ ->
        Text(
            text = getCount(item).toStartDoubleFormat(format),
            modifier = modifier
        )
    }
}

