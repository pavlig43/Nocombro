package ru.pavlig43.coreui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp


/**
 * Compose-компонент текстового поля с заголовком в колонке.
 *
 * Предоставляет текстовое поле для ввода строки с заголовком сверху.
 * Автоматически скрывает клавиатуру при нажатии Done/Готово.
 *
 * @param value Текущее значение текстового поля
 * @param onValueChange Callback при изменении значения
 * @param headText Текст заголовка, отображаемый над полем ввода
 */
@Composable
fun StringColumnField(
    value: String,
    onValueChange: (String) -> Unit,
    headText:String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = headText)
        val keyboardController = LocalSoftwareKeyboardController.current
        TextField(
            value = value,
            onValueChange = onValueChange,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                }
            )
        )
    }
}
