package ru.pavlig43.tablecore.ui

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import ua.wwind.table.component.TableCellTextFieldWithTooltipError
import kotlin.math.pow

private fun Int.toDoubleFormat(digitsAfterDot: Int): String {
    return (this / (10.0.pow(digitsAfterDot))).toString()

}

@Composable
fun TableCellTextFieldNumber(
    value: Int,
    saveInModel: (Int) -> Unit,
    countDigitsAfterDot: Int,
    onComplete: () -> Unit,
    errorMessage: String = "",
) {

    var displayValue by remember(value) { mutableStateOf(value.toDoubleFormat(countDigitsAfterDot)) }
    TableCellTextFieldWithTooltipError(
        value = displayValue,
        onValueChange = { input ->

//            val a = if (input.contains("[0-9]".toRegex()) || input.con)
            val lastChar = input.lastOrNull()
            val newText = when {
                input.substringAfter('.').length > countDigitsAfterDot -> input
                lastChar?.isDigit() == true -> input
                input.dropLast(1).contains('.') -> input.dropLast(1)
                else -> input.dropLast(1) + '.'
            }
            displayValue = newText

        },
        errorMessage = errorMessage,
        placeholder = { Text("") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(
                onDone = {
                    val doubleValue = displayValue.toDoubleOrNull() ?: 0.0

                    val intCount = (doubleValue * 10.0.pow(countDigitsAfterDot))
                    saveInModel(intCount.toInt())

                    onComplete()
                },
            ),
    )
}
