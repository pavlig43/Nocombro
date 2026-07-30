package ru.pavlig43.expense.internal.update.tabs.essential

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.expense.internal.ExpenseEssentialsCards

/** Показывает карточную форму правки расхода. */
@Composable
internal fun UpdateExpenseSingleLineScreen(
    component: ExpenseUpdateSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    ExpenseEssentialsCards(
        component = component,
        onOpenDateTimeDialog = component::onOpenDateTimeDialog,
    )

    dialog.child?.instance?.also {
        DateTimePickerDialog(it)
    }
}
