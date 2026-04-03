@file:Suppress("UNCHECKED_CAST")

package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.component.TableCellTextFieldWithTooltipError


@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.intRangeColumn(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    updateItem: (T, Int) -> Unit,
    range: IntRange,
    isSortable: Boolean = true,
    placeholder: String = range.first.toString(),
) {
    column(key, valueOf = { getValue(it) }) {
//        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        if (isSortable) {
            sortable()
        }

        cell { item, _ ->
            Text(
                text = getValue(item).toString(),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }

        editCell { item: T, _, onComplete ->
            TableCellIntRangeField(
                value = getValue(item),
                saveInModel = { updateItem(item, it) },
                onComplete = onComplete,
                range = range,
                placeholder = placeholder,
            )
        }
    }
}

@Composable
private fun TableCellIntRangeField(
    value: Int,
    saveInModel: (Int) -> Unit,
    onComplete: () -> Unit,
    range: IntRange,
    placeholder: String = range.first.toString(),
) {
    var displayValue by remember {
        mutableStateOf(if (value == range.first) "" else value.toString())
    }

    TableCellTextFieldWithTooltipError(
        value = displayValue,
        onValueChange = { input ->
            val digitsOnly = input.filter { it.isDigit() }
            val parsed = digitsOnly.toIntOrNull()

            if (parsed != null && parsed in range) {
                displayValue = digitsOnly
                saveInModel(parsed)
            } else if (digitsOnly.isEmpty()) {
                displayValue = ""
                saveInModel(range.first)
            }
        },
        errorMessage = "",
        placeholder = { Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        keyboardActions = KeyboardActions(
            onDone = { onComplete() },
        ),
    )
}
