package ru.pavlig43.mutable.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.coreui.toStartDoubleFormat
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import ua.wwind.table.filter.data.TableFilterType
import kotlin.math.pow


@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.decimalColumn(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    decimalFormat: DecimalFormat,
    updateItem: (T, Int) -> Unit,
    filterType: TableFilterType.NumberTableFilter<Int>?= null,
    isSortable: Boolean = true,
    footerValue: ((E) -> Int)? = null
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }

        cellForDecimalFormat(
            format = decimalFormat,
            getCount = { getValue(it) },
            saveInModel = updateItem
        )
        footerValue?.let { accumulateFunction ->
            footer { tableData ->
                val accumValue = accumulateFunction(tableData)
                Text(
                    text = accumValue.toStartDoubleFormat(decimalFormat),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        if (isSortable) {
            sortable()
        }
    }
}
@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readDecimalColumn(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    decimalFormat: DecimalFormat,
    filterType: TableFilterType.NumberTableFilter<Int>?= null,
    isSortable: Boolean = true
) {
    column(key, valueOf = { getValue(it) }) {
//        autoWidth(300.dp)
        header(headerText)
        filterType?.let {
            filter(it)
        }
        align(Alignment.CenterStart)
        readNumberCell(format = decimalFormat, getCount = { getValue(it) })
        if (isSortable) {
            sortable()
        }
    }
}
@Suppress("LongParameterList")
fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readDecimalColumnWithFooter(
    key: C,
    getValue: (T) -> Int,
    headerText: String,
    decimalFormat: DecimalFormat,
    footerValue: (E) -> Int,
    isSortable: Boolean = true
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filter(
            TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate,
            )
        )
        readNumberCell(format = decimalFormat, getCount = { getValue(it) })
        footer { tableData ->
            val accumValue = footerValue(tableData)
            Text(
                text = accumValue.toStartDoubleFormat(decimalFormat),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
        if (isSortable) {
            sortable()
        }
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.readNumberCell(
    format: DecimalFormat,
    getCount: (T) -> Int,
) {
    cell { item, _ ->
        LockText(
            text = getCount(item).toStartDoubleFormat(format),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.cellForDecimalFormat(
    format: DecimalFormat,
    getCount: (T) -> Int,
    saveInModel: (T, Int) -> Unit,

    ) {
    // Для отображения значения, когда оно не редактируется
    cell { item, _ ->
        Text(
            text = getCount(item).toStartDoubleFormat(format),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    editCell { item: T, tableData: E, onComplete: () -> Unit ->

        TableCellTextFieldNumber(
            value = getCount(item),
            saveInModel = {
                saveInModel(item, it)
            },
            decimalFormat = format,
            onComplete = onComplete,
        )
    }

}

/**
 * Пользователь вводит Double(подразумевая стандартные варианты измерения(кг или рубли))
 * колбэк берет это значение и сохраняет в модель как целочисленное(граммы или копейки)
 */
@Composable
private fun TableCellTextFieldNumber(
    value: Int,
    saveInModel: (Int) -> Unit,
    decimalFormat: DecimalFormat,
    onComplete: () -> Unit,
    errorMessage: String = "",
) {

    var error by remember { mutableStateOf(errorMessage) }
    // Отображаемое значение, должно быть всегда, хоть изменение и реактивное, но если введенное
    // значение нельзя привести к числовому формату то функция обновления не сработает
    var displayValue by remember {
        mutableStateOf(
            value.takeIf { it != 0 }?.toStartDoubleFormat(decimalFormat) ?: ""
        )
    }

    TableCellTextFieldWithTooltipError(
        value = displayValue,
        onValueChange = { input ->
            /**
             * Проверяется ввод посимвольно, берется пустая строка и к ней лепится (
             * если число - да
             * если еще не содержит точку - да
             * иначе пропускается символ
            )
             */
            val result = input.fold("") { acc: String, element: Char ->
                when {
                    element.isDigit() -> acc + element
                    !acc.contains('.') -> acc + '.'

                    else -> acc
                }
            }

            /**
             * Если точка есть, то обрезает дробную часть до количества знаков [DecimalFormat.countDecimal]
             */
            val parts = result.split('.')
            val finalText = if (parts.size == 2) {
                parts[0] + "." + parts[1].take(decimalFormat.countDecimal)
            } else result

            displayValue = finalText
            /**
             * Если введеный текст можно привести к Double, то происходит реактивное изменение модели
             * в компоненте через приведение к целочисленному значению.
             */
            if (displayValue.toDoubleOrNull() != null) {

                val intCount =
                    (displayValue.toDouble() * 10.0.pow(decimalFormat.countDecimal)).toInt()
                saveInModel(intCount)
                error = ""
            } else {
                error = "Не число"
            }


        },
        errorMessage = error,
        placeholder = { Text("") },
        singleLine = true,
        keyboardActions =
            KeyboardActions(
                onDone = {

                    onComplete()
                },
            ),
    )
}
