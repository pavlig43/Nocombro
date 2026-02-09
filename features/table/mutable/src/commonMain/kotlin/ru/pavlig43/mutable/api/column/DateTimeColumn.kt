package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.dateTimeFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.clock
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder

fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.writeDateTimeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDateTime,
    onOpenDateTimeDialog: () -> Unit,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(alignment)
        writeDateTimeCell(
            valueOf = valueOf,
            onOpenDateTimeDialog = onOpenDateTimeDialog
        )
    }
}

fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readDateTimeColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> LocalDateTime,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(alignment)
        readDateTimeCell(valueOf = valueOf)
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.readDateTimeCell(
    valueOf: (T) -> LocalDateTime,
) {
    cell { item, _ ->
        LockText(text = valueOf(item).format(dateTimeFormat))
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.writeDateTimeCell(
    valueOf: (T) -> LocalDateTime,
    onOpenDateTimeDialog: () -> Unit,
) {
    cell { item, _ ->
        WriteDateTimeRow(
            dateTime = valueOf(item),
            onOpenDateTimeDialog = onOpenDateTimeDialog
        )
    }
}

@Composable
private fun WriteDateTimeRow(
    dateTime: LocalDateTime,
    onOpenDateTimeDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolTipIconButton(
            tooltipText = "Дата/время",
            onClick = onOpenDateTimeDialog,
            icon = Res.drawable.clock

        )
        Text(dateTime.format(dateTimeFormat), textDecoration = TextDecoration.Underline)
    }
}

