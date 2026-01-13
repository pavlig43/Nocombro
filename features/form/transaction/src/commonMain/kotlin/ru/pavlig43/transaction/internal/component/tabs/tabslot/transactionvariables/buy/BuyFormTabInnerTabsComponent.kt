package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

internal class BuyFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    essentialFactory: EssentialComponentFactory<Transaction, TransactionEssentialsUi>,
    closeFormScreen: () -> Unit,
    scope: Scope,
    id: Int
) : ComponentContext by componentContext, IItemFormInnerTabsComponent<BuyTab, BuyFormSlot> {

    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<BuyTab, BuyFormSlot> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                BuyTab.Essentials,
//                BuyTab.BaseProduct
            ),
            serializer = BuyTab.serializer(),
            slotFactory = { context, tabConfig: BuyTab, _: () -> Unit ->
                when (tabConfig) {

                    BuyTab.Essentials -> BuyEssentialFormSlot(
                        componentContext = context,
                        componentFactory = essentialFactory,
                        id = id,
                        updateRepository = scope.get(),
                    )

//                    BuyTab.BaseProduct -> BuyBaseProductFormSlot(
//                        componentContext = context,
//
//                        )
                }

            },
        )

    private suspend fun update(): Result<Unit> {
        val blocks = tabNavigationComponent.tabChildren.map { children ->
            children.items.map { child -> suspend { child.instance.onUpdate() } }
        }
        return dbTransaction.transaction(blocks.value)

    }

    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        closeFormScreen = closeFormScreen,
        errorMessages = getErrors(lifecycle)
    )

}