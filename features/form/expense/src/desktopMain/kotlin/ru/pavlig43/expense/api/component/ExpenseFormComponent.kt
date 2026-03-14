package ru.pavlig43.expense.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.scope.Scope
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.di.createExpenseFormModule
import ru.pavlig43.expense.internal.component.ExpenseFormTabsComponent

class ExpenseFormComponent(
    componentContext: ComponentContext,
    dependencies: ExpenseFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createExpenseFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Расходы"))
    override val model = _model.asStateFlow()

    internal val expenseFormTabsComponent = ExpenseFormTabsComponent(
        componentContext = componentContext,
        scope = scope,
        observeOnExpense = { expense ->
            _model.update { MainTabComponent.NavTabState("Расходы") }
        }
    )

}
