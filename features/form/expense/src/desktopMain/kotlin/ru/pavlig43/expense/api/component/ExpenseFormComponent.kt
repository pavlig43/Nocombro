package ru.pavlig43.expense.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.create.component.CreateExpenseSingleLineComponent
import ru.pavlig43.expense.internal.di.createExpenseFormModule
import ru.pavlig43.expense.internal.model.ExpenseEssentialsUi
import ru.pavlig43.expense.internal.model.toUi
import ru.pavlig43.expense.internal.update.ExpenseFormTabsComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory

class ExpenseFormComponent(
    expenseId: Int,
    componentContext: ComponentContext,
    dependencies: ExpenseFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createExpenseFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    private val essentialsComponentFactory = SingleLineComponentFactory<ExpenseBD, ExpenseEssentialsUi>(
        initItem = ExpenseEssentialsUi(),
        errorFactory = { item: ExpenseEssentialsUi ->
            buildList {
                if (item.expenseType == null) add("Тип расхода обязателен")
                if (item.amount.value <= 0) add("Сумма должна быть больше 0")
            }
        },
        mapperToUi = { toUi() }
    )

    private fun onChangeValueForMainTab(expense: ExpenseEssentialsUi) {
        val title = "*Расход ${expense.expenseType?.displayName ?: ""}"
        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateExpenseSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    componentFactory = essentialsComponentFactory,
                    createExpenseRepository = scope.get(),
                    observeOnItem = { expense -> onChangeValueForMainTab(expense) }
                )
            )

            is Config.Update -> Child.Update(
                ExpenseFormTabsComponent(
                    componentContext = componentContext,
                    scope = scope,
                    expenseId = config.id,
                    componentFactory = essentialsComponentFactory,
                    observeOnExpense = ::onChangeValueForMainTab
                )
            )
        }
    }

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (expenseId == 0) Config.Create else Config.Update(expenseId),
        handleBackButton = false,
        childFactory = ::createChild
    )

    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Create(val component: CreateExpenseSingleLineComponent) : Child()
        class Update(val component: ExpenseFormTabsComponent) : Child()
    }
}
