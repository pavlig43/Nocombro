package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType

fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.writeCheckBoxColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> Boolean,
    onChangeChecked: (T, Boolean) -> Unit,
    filterType: TableFilterType.BooleanTableFilter? = null,
    alignment: Alignment = Alignment.Center
) {
    column(column, valueOf = valueOf) {
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        writeCheckBoxCell(
            valueOf = valueOf,
            onCheckedChange = onChangeChecked
        )
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.writeCheckBoxCell(
    valueOf: (T) -> Boolean,
    onCheckedChange: (T, Boolean) -> Unit,
) {
    cell { item, _ ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = valueOf(item),
                onCheckedChange = { newValue -> onCheckedChange(item, newValue) }
            )
        }
    }
}
