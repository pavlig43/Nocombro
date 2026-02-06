package ru.pavlig43.transaction.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.launch
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.transaction.internal.component.tabs.component.TransactionEssentialComponent
import ru.pavlig43.transaction.internal.component.tabs.component.buy.BuyComponent
import ru.pavlig43.transaction.internal.component.tabs.component.reminders.RemindersComponent
import ru.pavlig43.transaction.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class TransactionFormTabsComponent(
    componentContext: ComponentContext,
    essentialFactory: EssentialComponentFactory<Transaction, TransactionEssentialsUi>,
    closeFormScreen: () -> Unit,
    transactionId: Int,
    tabOpener: TabOpener,
    scope: Scope
): ComponentContext by componentContext, IItemFormTabsComponent<TransactionTab, TransactionTabChild>{
    private val coroutineScope = componentCoroutineScope()

    override val transactionExecutor: TransactionExecutor = scope.get()

    override val tabNavigationComponent: TabNavigationComponent<TransactionTab, TransactionTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(TransactionTab.Essentials),
            serializer = TransactionTab.serializer(),
            tabChildFactory = {context, config, closeTab ->
                when(config){
                    TransactionTab.Essentials -> TransactionTabChild.Essentials(
                        TransactionEssentialComponent(
                            componentContext = context,
                            id = transactionId,
                            updateRepository = scope.get(),
                            componentFactory = essentialFactory,
                            onSuccessInitData = {transaction->
                                coroutineScope.launch {
                                    when(transaction.transactionType){
                                        TransactionType.BUY -> {
                                            tabNavigationComponent.addTab(TransactionTab.Buy)
                                        }
                                        TransactionType.SALE -> TODO()
                                        TransactionType.OPZS -> TODO()
                                        TransactionType.WRITE_OFF -> TODO()
                                        TransactionType.INVENTORY -> TODO()
                                        null -> TODO()
                                    }
                                    // Добавляем вкладку Напоминания для всех типов транзакций
                                    tabNavigationComponent.addTab(TransactionTab.Reminders)
                                }
                            }
                        )
                    )
                    TransactionTab.Buy -> TransactionTabChild.Buy(
                        BuyComponent(
                            componentComponent = context,
                            transactionId = transactionId,
                            repository = scope.get(UpdateCollectionRepositoryType.BUY.qualifier),
                            tabOpener = tabOpener,
                            immutableTableDependencies = scope.get()
                        )
                    )
                    TransactionTab.Reminders -> TransactionTabChild.Reminders(
                        RemindersComponent(
                            componentContext = context,
                            transactionId = transactionId,
                            repository = scope.get(UpdateCollectionRepositoryType.REMINDERS.qualifier)
                        )
                    )

                }
            }
        )
    override val updateComponent: UpdateComponent = getDefaultUpdateComponent(componentContext,closeFormScreen)

}