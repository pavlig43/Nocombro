package ru.pavlig43.transaction.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.transaction.internal.component.tabs.TransactionTab
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

internal class TransactionFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    essentialFactory: EssentialComponentFactory<ProductTransaction, TransactionEssentialsUi>,
    closeFormScreen:()->Unit,
    scope: Scope,
    id: Int
) : ComponentContext by componentContext, IItemFormInnerTabsComponent<TransactionTab, TransactionFormSlot> {

    private val dbTransaction:DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<TransactionTab, TransactionFormSlot> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                TransactionTab.Essentials,
            ),
            serializer = TransactionTab.serializer(),
            slotFactory = { context, tabConfig: TransactionTab,  _: () -> Unit ->
                when (tabConfig) {

                    TransactionTab.Essentials -> EssentialFormSlot(
                        componentContext = context,
                        componentFactory = essentialFactory,
                        documentId = id,
                        updateRepository = scope.get(),
                    )


                }

            },
        )
    private suspend fun update():Result<Unit> {
        val blocks= tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        return dbTransaction.transaction(blocks.value)

    }
    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )

}
