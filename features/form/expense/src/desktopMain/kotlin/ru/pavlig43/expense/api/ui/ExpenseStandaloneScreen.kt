package ru.pavlig43.expense.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.expense.api.component.ExpenseFormComponent
import ru.pavlig43.expense.internal.component.ExpenseTabChild
import ru.pavlig43.expense.internal.component.tabs.table.TableScreen
import ru.pavlig43.update.ui.FormTabsUi

@Composable
fun ExpenseStandaloneScreen(component: ExpenseFormComponent) {

    FormTabsUi(
        component = component.expenseFormTabsComponent,
        tabChildFactory = { tabChild ->
            when (tabChild) {
                is ExpenseTabChild.Expenses -> {
                    TableScreen(tabChild.component)

                }
                null -> Unit
            }
        }
    )
}
