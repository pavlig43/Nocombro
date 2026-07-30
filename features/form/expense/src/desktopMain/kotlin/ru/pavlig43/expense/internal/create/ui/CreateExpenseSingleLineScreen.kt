package ru.pavlig43.expense.internal.create.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.expense.internal.ExpenseEssentialsCards
import ru.pavlig43.expense.internal.create.component.CreateExpenseSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen

/** Показывает карточную форму создания расхода. */
@Composable
internal fun CreateExpenseSingleLineScreen(
    component: CreateExpenseSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    CreateSingleItemScreen(
        component = component,
        itemContent = { modifier ->
            ExpenseEssentialsCards(
                component = component,
                onOpenDateTimeDialog = component::onOpenDateTimeDialog,
                modifier = modifier,
            )
        },
    )

    dialog.child?.instance?.also { dateTimeComponent ->
        DateTimePickerDialog(dateTimeComponent)
    }
}
