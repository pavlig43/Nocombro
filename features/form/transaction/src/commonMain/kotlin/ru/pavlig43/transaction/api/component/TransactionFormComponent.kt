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
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.component.CreateTransactionComponent
import ru.pavlig43.transaction.internal.component.tabs.tabslot.TransactionFormTabInnerTabsComponent
import ru.pavlig43.transaction.internal.di.createTransactionFormModule
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.model.toUi

class TransactionFormComponent(
    transactionId: Int,
    val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: TransactionFormDependencies,
) : ComponentContext by componentContext, SlotComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createTransactionFormModule(dependencies))


    private val _model = MutableStateFlow(SlotComponent.TabModel(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()


    private val essentialFactory =
        EssentialComponentFactory<ProductTransaction, TransactionEssentialsUi>(
            initItem = TransactionEssentialsUi(),
            isValidValuesFactory = { transactionType != null && operationType != null && createdAt != null },
            mapperToUi = { toUi() },
            vendorInfoForTabName = { onChangeValueForMainTab("*Операция ${it.transactionType}") }
        )

    private fun onChangeValueForMainTab(title: String) {

        val tabModel = SlotComponent.TabModel(title)
        _model.update { tabModel }
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
                TransactionFormTabInnerTabsComponent(
                    componentContext = componentContext,
                    essentialFactory = essentialFactory,
                    scope = scope,
                    id = config.id,
                    closeFormScreen = closeTab
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
        class Update(val component: TransactionFormTabInnerTabsComponent) : Child()
    }
}
