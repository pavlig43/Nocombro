package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.filter.data.TableFilterType

@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.writeTextColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> String,
    onChangeItem: (T, String) -> Unit,
    filterType: TableFilterType.TextTableFilter? = null,
    singleLine: Boolean = true,
    alignment: Alignment = Alignment.CenterStart
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        writeTextCell(
            valueOf = valueOf,
            singleLine = singleLine,
            onValueChange = onChangeItem
        )
        sortable()
    }
}

fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readTextColumn(
    headerText: String,
    column: C,
    valueOf: (T) -> String,
    filterType: TableFilterType<*>? = null,
    alignment: Alignment = Alignment.CenterStart
) {
    column(column, valueOf = valueOf) {
        autoWidth(300.dp)
        header(headerText)
        align(alignment)
        filterType?.let {
            filter(it)
        }
        readTextCell(valueOf = valueOf)
        sortable()
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.readTextCell(
    valueOf: (T) -> String,
) {
    cell { item, _ ->
        LockText(text = valueOf(item))
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.writeTextCell(
    valueOf: (T) -> String,
    onValueChange: (T, String) -> Unit,
    singleLine: Boolean = true,
) {
    cell { item, _ ->
        Text(
            text = valueOf(item),
            modifier = Modifier.padding(12.dp)
        )
    }

    editCell { item, _, onComplete ->
        TableCellTextField(
            value = valueOf(item),
            onValueChange = { newValue -> onValueChange(item, newValue) },
            singleLine = singleLine,
            keyboardActions = KeyboardActions(onDone = { onComplete() })
        )
    }
}
