package ru.pavlig43.mutable.api.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ua.wwind.table.EditableColumnBuilder
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import kotlin.math.pow


sealed interface DecimalFormat {
    val countDecimal: Int

    class KG : DecimalFormat {
        override val countDecimal: Int = 3
    }

    class RUB : DecimalFormat {
        override val countDecimal: Int = 2
    }
}
fun<T: Any,C,E> EditableColumnBuilder<T,C,E>.cellForDecimalFormat(
    format: DecimalFormat,
    getCount:(T)-> Int,
    saveInModel: (T,Int) -> Unit
){
    // Для отображения значения, когда оно не редактируется
    cell { item, _ -> Text(getCount(item).toStartDoubleFormat(format)) }

    editCell { item: T, tableData: E, onComplete: () -> Unit ->

        TableCellTextFieldNumber(
            value = getCount(item),
            saveInModel = {
                saveInModel(item,it)
            },
            decimalFormat = format,
            onComplete = onComplete,
        )
    }
}
private fun Int.toStartDoubleFormat(decimalFormat: DecimalFormat): String {
    return (this / (10.0.pow(decimalFormat.countDecimal))).toString()
        .dropLastWhile { it == '0' }
        .run { if (last() == '.') dropLast(1) else this }
}

/**
 * Пользователь вводит Double(подразумевая стандартные варианты измерения(кг или рубли))
 * колбэк берет это значение и сохраняет в модель как целочисленное(граммы или копейки)
 */
@Composable
fun TableCellTextFieldNumber(
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
            }
            else {
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
