package ru.pavlig43.expense.internal.create.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.expense.internal.create.component.CreateExpenseSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen

/**
 * UI экран для создания расхода через таблицу с одной строкой
 *
 * @param component Компонент создания расхода
 */
@Composable
internal fun CreateExpenseSingleLineScreen(
    component: CreateExpenseSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        CreateSingleItemScreen(component)
    }

    // Отображение диалога выбора даты/времени
    dialog.child?.instance?.also { dateTimeComponent ->
        DateTimePickerDialog(dateTimeComponent)
    }
}
