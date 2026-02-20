package ru.pavlig43.transaction.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.transaction.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.transaction.internal.di.UpdateSingleLineRepositoryType
import ru.pavlig43.transaction.internal.model.TransactionEssentialsUi
import ru.pavlig43.transaction.internal.update.tabs.component.buy.BuyComponent
import ru.pavlig43.transaction.internal.update.tabs.component.expenses.ExpensesComponent
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf.PfComponent
import ru.pavlig43.transaction.internal.update.tabs.component.reminders.RemindersComponent
import ru.pavlig43.transaction.internal.update.tabs.essential.TransactionUpdateSingleLineComponent
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

@Suppress("LongParameterList")
internal class TransactionFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Transact, TransactionEssentialsUi>,
    scope: Scope,
    transactionId: Int,
    private val observeOnItem: (TransactionEssentialsUi) -> Unit,
    private val tabOpener: TabOpener,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<TransactionTab, TransactionTabChild> {
    private val coroutineScope = componentCoroutineScope()

    override val transactionExecutor: TransactionExecutor = scope.get()
    private val essentialsFields = MutableStateFlow<TransactionEssentialsUi>(componentFactory.initItem)

    private fun observeOnTransaction(transaction: TransactionEssentialsUi) {
        observeOnItem(transaction)
        essentialsFields.update { transaction }
    }

    private fun onSuccessInitTransaction(transaction: TransactionEssentialsUi) {
        observeOnTransaction(transaction)
        coroutineScope.launch {
            when(transaction.transactionType){
                TransactionType.BUY -> tabNavigationComponent.addTab(TransactionTab.Buy)
                TransactionType.SALE -> TODO()
                TransactionType.OPZS -> tabNavigationComponent.addTab(TransactionTab.Pf)
                TransactionType.WRITE_OFF -> TODO()
                TransactionType.INVENTORY -> TODO()
                null -> throw IllegalArgumentException("Transaction type is null")
            }
            tabNavigationComponent.onSelectTab(0)
        }
    }

    override val tabNavigationComponent: TabNavigationComponent<TransactionTab, TransactionTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                TransactionTab.Essentials,
                TransactionTab.Expenses,
                TransactionTab.Reminders,
            ),
            serializer = TransactionTab.serializer(),
            tabChildFactory = { context, config, _ ->
                when (config) {
                    TransactionTab.Essentials -> TransactionTabChild.Essentials(
                        TransactionUpdateSingleLineComponent(
                            componentContext = context,
                            transactionId = transactionId,
                            updateRepository = scope.get(UpdateSingleLineRepositoryType.TRANSACTION.qualifier),
                            componentFactory = componentFactory,
                            observeOnItem = ::observeOnTransaction,
                            onSuccessInitData = ::onSuccessInitTransaction
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

                    TransactionTab.Expenses -> TransactionTabChild.Expenses(
                        ExpensesComponent(
                            componentContext = context,
                            transactionId = transactionId,
                            getTransactionDateTime = {essentialsFields.value.createdAt},
                            repository = scope.get(UpdateCollectionRepositoryType.EXPENSES.qualifier),
                        )
                    )

                    TransactionTab.Pf -> {
                        TransactionTabChild.Pf(
                            PfComponent(
                                componentContext = context,
                                transactionId = transactionId,
                                updateSingleLineRepository = scope.get(UpdateSingleLineRepositoryType.PF.qualifier),
                                tabOpener = tabOpener,
                                getDateBorn = {essentialsFields.value.createdAt.date},
                                immutableTableDependencies = scope.get()
                            )
                        )
                    }
                }
            }
        )

    override val updateComponent: UpdateComponent =
        getDefaultUpdateComponent(componentContext)
}
