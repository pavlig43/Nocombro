package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.calendar
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.writeDateColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDate,
    onOpenDateDialog: (T) -> Unit,
    filterType: TableFilterType.DateTableFilter? = null,
    isSortable: Boolean = true,
    alignment: Alignment = Alignment.CenterStart
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        writeDateCell(
            valueOf = valueOf,
            onOpenDateDialog = onOpenDateDialog
        )
        if (isSortable) {
            sortable()
        }
    }
}
@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readDateColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDate,
    filterType: TableFilterType<*>? = null,
    isSortable: Boolean = true,
    alignment: Alignment = Alignment.CenterStart
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        readDateCell(valueOf = valueOf)
        if (isSortable) {
            sortable()
        }
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.readDateCell(
    valueOf: (T) -> LocalDate,
) {
    cell { item, _ ->
        LockText(text = valueOf(item).format(dateFormat))
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.writeDateCell(
    valueOf: (T) -> LocalDate,
    onOpenDateDialog: (T) -> Unit,
) {
    cell { item, _ ->
        WriteDateRow(
            date = valueOf(item),
            onOpenDateDialog = { onOpenDateDialog(item) }
        )
    }
}

@Composable
private fun WriteDateRow(
    date: LocalDate,
    onOpenDateDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTipIconButton(
            tooltipText = "Дата",
            onClick = onOpenDateDialog,
            icon = Res.drawable.calendar

        )

        Text(date.format(dateFormat), textDecoration = TextDecoration.Underline)
    }
}
