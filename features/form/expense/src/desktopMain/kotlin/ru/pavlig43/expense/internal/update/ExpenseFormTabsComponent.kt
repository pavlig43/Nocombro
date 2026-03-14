package ru.pavlig43.expense.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.expense.internal.update.tabs.files.ExpenseFilesComponent
import ru.pavlig43.expense.internal.update.tabs.essential.ExpenseUpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class ExpenseFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<ExpenseBD, ExpenseEssentialsUi>,
    scope: Scope,
    expenseId: Int,
    observeOnExpense: (ExpenseEssentialsUi) -> Unit,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<ExpenseTab, ExpenseTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()

    override val tabNavigationComponent: TabNavigationComponent<ExpenseTab, ExpenseTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ExpenseTab.Essentials,
                ExpenseTab.Files
            ),
            serializer = ExpenseTab.serializer(),
            tabChildFactory = { context, tabConfig: ExpenseTab, _: () -> Unit ->
                when (tabConfig) {
                    ExpenseTab.Essentials -> ExpenseTabChild.Essentials(
                        ExpenseUpdateSingleLineComponent(
                            componentContext = context,
                            expenseId = expenseId,
                            updateRepository = scope.get(),
                            observeOnItem = observeOnExpense,
                            onSuccessInitData = observeOnExpense,
                            componentFactory = componentFactory
                        )
                    )
                    ExpenseTab.Files -> ExpenseTabChild.Files(
                        ExpenseFilesComponent(
                            componentContext = context,
                            expenseId = expenseId,
                            dependencies = scope.get()
                        )
                    )
                }
            },
        )

    override val updateComponent = getDefaultUpdateComponent(componentContext)
}
