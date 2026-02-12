package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import androidx.compose.runtime.Composable
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox

@Composable
internal fun ExpensesScreen(
    component: ExpensesComponent
) {
    MutableTableBox(component)
}
