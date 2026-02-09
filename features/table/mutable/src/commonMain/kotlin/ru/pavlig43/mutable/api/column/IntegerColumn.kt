package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import ru.pavlig43.tablecore.model.ITableUi
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.filter.data.TableFilterType

fun <T : ITableUi, C, E> EditableTableColumnsBuilder<T, C, E>.intColumn(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    updateItem: (T, Int) -> Unit,
    alignment: Alignment = Alignment.Center
) {
    column(key, valueOf = { getValue(it) }) {
        header(headerText)
        align(alignment)
        filter(
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
            )
        )
        cellForInt(
            getValue = getValue,
            saveInModel = updateItem
        )
        sortable()
    }
}

fun <T : ITableUi, C, E> EditableTableColumnsBuilder<T, C, E>.readIntColumn(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    alignment: Alignment = Alignment.Center
) {
    column(key, valueOf = { getValue(it) }) {
        header(headerText)
        align(alignment)
        readIntCell(getValue = getValue)
    }
}

private fun <T : ITableUi, C, E> EditableColumnBuilder<T, C, E>.readIntCell(
    getValue: (T) -> Int,
) {
    cell { item, _ ->
        LockText(text = getValue(item).toString())
    }
}

private fun <T : ITableUi, C, E> EditableColumnBuilder<T, C, E>.cellForInt(
    getValue: (T) -> Int,
    saveInModel: (T, Int) -> Unit,
) {
    cell { item, _ -> Text(getValue(item).toString()) }

    editCell { item, _, onComplete ->
        TableCellTextField(
            value = getValue(item).toString(),
            onValueChange = { newValue ->
                newValue.toIntOrNull()?.let { saveInModel(item, it) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(onDone = { onComplete() })
        )
    }
}
