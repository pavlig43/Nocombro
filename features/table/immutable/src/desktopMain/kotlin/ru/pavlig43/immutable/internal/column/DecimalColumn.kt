package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import ru.pavlig43.immutable.internal.ui.HighlightedTableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.model.DecimalData
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType



@Suppress("LongParameterList")
fun <T : Any, C, E,DECIMAL: DecimalData> ReadonlyTableColumnsBuilder<T, C, E>.readDecimalColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> DECIMAL,
    alignment: Alignment = Alignment.Center,
    textModifier: Modifier = Modifier.padding(horizontal = tableCellHorizontalPadding),
    filterType: TableFilterType.NumberTableFilter<DECIMAL>? = null,
    isSortable: Boolean = true,
    footerContent: @Composable (BoxScope.(E) -> Unit)? = null
) {
    column(column, valueOf = { valueOf(it) }) {
        autoWidth(300.dp)
        header(headerText)
        filterType?.let {
            filter(it)
        }
        align(alignment)
        readDecimalCell(getCount = valueOf,modifier = textModifier)
        if (isSortable){
            sortable()
        }
        footerContent?.let {
            footer(it)
        }
    }
}

private fun <T : Any, C, E,DECIMAL: DecimalData> ReadonlyColumnBuilder<T, C, E>.readDecimalCell(
    getCount: (T) -> DECIMAL,
    modifier: Modifier
) {
    cell { item, _ ->
        HighlightedTableText(
            text = getCount(item).toTableDisplayText(),
            modifier = modifier
        )
    }
}

