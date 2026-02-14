package ru.pavlig43.transaction.api.component

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
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.create.component.CreateTransactionSingleLineComponent
import ru.pavlig43.transaction.internal.di.createTransactionFormModule
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toUi
import ru.pavlig43.transaction.internal.update.TransactionFormTabsComponent

class TransactionFormComponent(
    transactionId: Int,
    val closeTab: () -> Unit,
    private val tabOpener: TabOpener,
    componentContext: ComponentContext,
    dependencies: TransactionFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }

    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createTransactionFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    private val componentFactory = SingleLineComponentFactory<Transact, TransactionEssentialsUi>(
        initItem = TransactionEssentialsUi(),
        errorFactory = { item: TransactionEssentialsUi ->
            buildList {
                if (item.transactionType == null) add("Тип транзакции обязателен")
                if (item.createdAt == emptyDate) add("Дата/время обязательна")
            }
        },
        mapperToUi = { toUi() }
    )

    private fun onChangeValueForMainTab(transaction: TransactionEssentialsUi) {
        val title = "*Транзакция ${transaction.transactionType?.displayName ?: ""}"
        _model.update { MainTabComponent.NavTabState(title) }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateTransactionSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    observeOnItem = { transaction -> onChangeValueForMainTab(transaction) },
                    componentFactory = componentFactory,
                    createRepository = scope.get()
                )
            )

            is Config.Update -> Child.Update(
                TransactionFormTabsComponent(
                    componentContext = componentContext,
                    scope = scope,
                    transactionId = config.id,
                    closeFormScreen = closeTab,
                    componentFactory = componentFactory,
                    tabOpener = tabOpener,
                    observeOnItem = ::onChangeValueForMainTab
                )
            )
        }
    }

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (transactionId == 0) Config.Create else Config.Update(
            transactionId
        ),
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
        class Create(val component: CreateTransactionSingleLineComponent) : Child()
        class Update(val component: TransactionFormTabsComponent) : Child()
    }
}
