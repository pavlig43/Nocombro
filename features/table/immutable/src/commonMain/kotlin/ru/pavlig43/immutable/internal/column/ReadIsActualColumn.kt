package ru.pavlig43.immutable.internal.column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.close
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

internal fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.readIsActualColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Boolean,
    filterType: TableFilterType.BooleanTableFilter? = null,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        readIsActualCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> ReadonlyColumnBuilder<T, C, E>.readIsActualCell(
    valueOf: (T) -> Boolean,
) {
    cell { item, _ ->
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isActual = valueOf(item)
            Icon(
                painter = painterResource(if (isActual) Res.drawable.check else Res.drawable.close),
                contentDescription = null,
                tint = if (isActual) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp)
            )
        }
    }
}
