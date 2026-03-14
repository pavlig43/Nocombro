package ru.pavlig43.expense.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.expense.internal.component.tabs.table.TableComponent
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class ExpenseFormTabsComponent(
    componentContext: ComponentContext,
    expenseId: Int,
    scope: Scope,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<ExpenseTab, ExpenseTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()

    override val tabNavigationComponent: TabNavigationComponent<ExpenseTab, ExpenseTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ExpenseTab.Expenses
            ),
            serializer = ExpenseTab.serializer(),
            tabChildFactory = { context, tabConfig: ExpenseTab, _: () -> Unit ->
                when (tabConfig) {
                    ExpenseTab.Expenses -> ExpenseTabChild.Expenses(
                        TableComponent(
                            componentContext = context,
                            repository = scope.get(),
                            expenseId = expenseId
                        )
                    )
                }
            },
        )

    override val updateComponent = getDefaultUpdateComponent(componentContext)
}
