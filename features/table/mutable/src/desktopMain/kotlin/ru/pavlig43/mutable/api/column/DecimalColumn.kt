@file:Suppress("UNCHECKED_CAST")

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
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.toStartDoubleFormat
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import ua.wwind.table.filter.data.TableFilterType
import kotlin.math.pow


@Suppress("LongParameterList")
fun <T : Any, C, E,DECIMAL: DecimalData> EditableTableColumnsBuilder<T, C, E>.decimalColumn(
    key: C,
    getValue: (T) -> DECIMAL,
    headerText: String,
    updateItem: (T, DECIMAL) -> Unit,
    filterType: TableFilterType.NumberTableFilter<DECIMAL>?= null,
    isSortable: Boolean = true,
    footerValue: ((E) -> DECIMAL)? = null
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }

        cellForDecimalFormat(
            getCount = { getValue(it) },
            saveInModel = updateItem
        )
        footerValue?.let { accumulateFunction ->
            footer { tableData ->
                val accumValue = accumulateFunction(tableData)
                Text(
                    text = accumValue.toStartDoubleFormat(),
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
fun <T : Any, C, E, DECIMAL: DecimalData> EditableTableColumnsBuilder<T, C, E>.readDecimalColumn(
    key: C,
    getValue: (T) -> DECIMAL,
    headerText: String,
    footerValue: ((E) -> DECIMAL)? = null,
    filterType: TableFilterType.NumberTableFilter<DECIMAL>?= null,
    isSortable: Boolean = true
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)
        filterType?.let {
            filter(it)
        }
        readNumberCell( getCount = { getValue(it) })
        footerValue?.let { accumulateFunction ->
            footer { tableData ->
                val accumValue = accumulateFunction(tableData)
                Text(
                    text = accumValue.toStartDoubleFormat(),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
        if (isSortable) {
            sortable()
        }
    }
}

private fun <T : Any, C, E> EditableColumnBuilder<T, C, E>.readNumberCell(
    getCount: (T) -> DecimalData,
) {
    cell { item, _ ->
        LockText(
            text = getCount(item).toStartDoubleFormat(),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

private fun <T : Any, C, E,DECIMAL: DecimalData> EditableColumnBuilder<T, C, E>.cellForDecimalFormat(
    getCount: (T) -> DECIMAL,
    saveInModel: (T, DECIMAL) -> Unit,

    ) {
    // Для отображения значения, когда оно не редактируется
    cell { item, _ ->
        Text(
            text = getCount(item).toStartDoubleFormat(),
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }

    editCell { item: T, tableData: E, onComplete: () -> Unit ->

        TableCellTextFieldNumber(
            data = getCount(item),
            saveInModel = {
                saveInModel(item, it)
            },
            onComplete = onComplete,
        )
    }

}

/**
 * Пользователь вводит Double(подразумевая стандартные варианты измерения(кг или рубли))
 * колбэк берет это значение и сохраняет в модель как целочисленное(граммы или копейки)
 */
@Composable
private fun<DECIMAL: DecimalData> TableCellTextFieldNumber(
    data: DECIMAL,
    saveInModel: (DECIMAL) -> Unit,
    onComplete: () -> Unit,
    errorMessage: String = "",
) {

    var error by remember { mutableStateOf(errorMessage) }
    // Отображаемое значение, должно быть всегда, хоть изменение и реактивное, но если введенное
    // значение нельзя привести к числовому формату то функция обновления не сработает
    var displayValue by remember {
        mutableStateOf(
            data.takeIf { it.value != 0 }?.toStartDoubleFormat() ?: ""
        )
    }

    TableCellTextFieldWithTooltipError(
        value = displayValue,
        onValueChange = { input ->
            /**
             * Проверяется ввод посимвольно, берется пустая строка и к ней лепится
             * (если число - да
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
                parts[0] + "." + parts[1].take(data.countDecimal)
            } else result

            displayValue = finalText
            /**
             * Если введеный текст можно привести к Double, то происходит реактивное изменение модели
             * в компоненте через приведение к целочисленному значению.
             */
            if (displayValue.toDoubleOrNull() != null) {

                val intCount =
                    (displayValue.toDouble() * 10.0.pow(data.countDecimal)).toInt()
                saveInModel(data.copyValue(value = intCount) as DECIMAL)
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
