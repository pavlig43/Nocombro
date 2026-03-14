package ru.pavlig43.expense.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.dateTime.DateTimePickerDialog
import ru.pavlig43.mutable.api.singleLine.ui.SingleLineBlockScreen

@Composable
internal fun UpdateExpenseSingleLineScreen(
    component: ExpenseUpdateSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()
    SingleLineBlockScreen(component)
    dialog.child?.instance?.also {
        DateTimePickerDialog(it)
    }
}
