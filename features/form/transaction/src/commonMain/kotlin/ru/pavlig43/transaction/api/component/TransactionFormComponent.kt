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
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.immutable.internal.component.items.transaction.TransactionTableUi
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.component.CreateTransactionComponent
import ru.pavlig43.transaction.internal.component.tabs.TransactionFormTabsComponent
import ru.pavlig43.transaction.internal.di.createTransactionFormModule
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toUi

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



    private val essentialFactory =
        EssentialComponentFactory<Transaction, TransactionEssentialsUi>(
            initItem = TransactionEssentialsUi(),
            isValidFieldsFactory = { transactionType != null },
            mapperToUi = { toUi() },
            produceInfoForTabName = { transaction: TransactionEssentialsUi ->
                onChangeValueForMainTab(
                    transaction.transactionType?.displayName ?: "* Транзакция"
                )
            }
        )
    private fun onChangeValueForMainTab(title: String) {

        val navTabState = MainTabComponent.NavTabState(title)
        _model.update { navTabState }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateTransactionComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    createRepository = scope.get(),
                    componentFactory = essentialFactory
                )

            )

            is Config.Update -> Child.Update(
                TransactionFormTabsComponent(
                    componentContext = componentContext,
                    essentialFactory = essentialFactory,
                    scope = scope,
                    transactionId = config.id,
                    closeFormScreen = closeTab,
                    tabOpener = tabOpener
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
        class Create(val component: CreateTransactionComponent) : Child()
        class Update(val component: TransactionFormTabsComponent) : Child()
    }
}
