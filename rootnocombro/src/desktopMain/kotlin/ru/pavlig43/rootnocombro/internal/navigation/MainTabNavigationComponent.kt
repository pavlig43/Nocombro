package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.declaration.api.DeclarationFormComponent
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.expense.api.component.ExpenseFormComponent
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.DocumentImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ExpenseImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ImmutableTableBuilderData
import ru.pavlig43.immutable.api.component.ImmutableTableComponentFactoryMain
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.api.component.SafetyImmutableTableBuilder
import ru.pavlig43.immutable.api.component.TransactionImmutableTableBuilder
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.expense.ExpenseTableUi
import ru.pavlig43.main.api.component.AnalyticMainComponent
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.doctor.api.component.DoctorComponent
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.profitability.internal.component.ProfitabilityComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.BatchMovementChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.DoctorChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ImmutableTableChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DeclarationFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DocumentFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.ExpenseFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.ProductFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.TransactionFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.VendorFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.MainMoneyChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.NotificationChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ProfitabilityChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.SampleTableChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.StorageChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.AnalyticConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.BatchMovementListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DeclarationFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DocumentFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.ExpenseFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.ProductFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.TransactionFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.VendorFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DeclarationListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DocumentListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.ExpenseListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.ProductListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.SafetyListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.TransactionListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.VendorListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.DoctorConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.NotificationConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ProfitabilityConfig
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.sampletable.api.component.SampleTableComponentMain
import ru.pavlig43.storage.api.component.batchMovement.BatchMovementComponent
import ru.pavlig43.storage.api.component.storage.StorageComponent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.vendor.api.component.VendorFormComponent

internal class MainTabNavigationComponent(
    componentContext: ComponentContext,
    private val scope: Scope,
) : ComponentContext by componentContext {

    private val notificationComponent = NotificationComponent(
        componentContext = childContext("notification"),
        onOpenTab = ::openTabFromNotification,
        dependencies = scope.get()
    )

    val drawerComponent: DrawerComponent = DrawerComponent(
        componentContext = childContext("drawer"),
        openScreen = ::openScreenFromDrawer,
        onNotificationScreen = { tabNavigationComponent.addTab(NotificationConfig()) },
        notificationsState = notificationComponent.notificationsForDrawer
    )

    fun openScreenFromDrawer(destination: DrawerDestination) {
        val mainTabConfiguration: MainTabConfig = destination.toMainTabConfig()
        tabNavigationComponent.addTab(mainTabConfiguration)
    }

    // Создаём tabOpener раньше, чем tabNavigationComponent
    private val tabOpener: TabOpener = createMainTabOpener { tabNavigationComponent.addTab(it) }

    val tabNavigationComponent: TabNavigationComponent<MainTabConfig, MainTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                ProfitabilityConfig(),
            ),
            serializer = MainTabConfig.serializer(),
            tabChildFactory = { context, mainTabConfig: MainTabConfig, onCloseTab: () -> Unit ->

                when (mainTabConfig) {
                    is NotificationConfig -> NotificationChild(notificationComponent)

                    is AnalyticConfig -> MainMoneyChild(
                        AnalyticMainComponent(
                            componentContext = context,
                            tabOpener = tabOpener,
                        )
                    )

                    is MainTabConfig.SampleTableConfig -> SampleTableChild(
                        SampleTableComponentMain(
                            componentContext = context
                        )
                    )

                    is MainTabConfig.StorageConfig -> {
                        StorageChild(
                            StorageComponent(
                                componentContext = context,
                                dependencies = scope.get(),
                                tabOpener = tabOpener
                            )
                        )
                    }

                    is ProfitabilityConfig -> {
                        ProfitabilityChild(
                            ProfitabilityComponent(
                                componentContext = context,
                                dependencies = scope.get()
                            )
                        )
                    }

                    is DoctorConfig -> {
                        DoctorChild(
                            DoctorComponent(
                                componentContext = context,
                                dependencies = scope.get(),
                            )
                        )
                    }

                    is BatchMovementListConfig -> {
                        BatchMovementChild(
                            BatchMovementComponent(
                                componentContext = context,
                                dependencies = scope.get(),
                                tabOpener = tabOpener,
                                batchId = mainTabConfig.batchId,
                                productName = mainTabConfig.productName,
                                initStart = mainTabConfig.start,
                                initEnd = mainTabConfig.end,
                            )
                        )
                    }

                    is MainTabConfig.ItemListConfig -> createImmutableTableChild(
                        tabConfig = mainTabConfig,
                        context = context
                    )


                    is MainTabConfig.ItemFormConfig -> createItemFormChild(
                        tabConfig = mainTabConfig,
                        context = context
                    )

                }
            },
    )

    private fun openTabFromNotification(item: NotificationItem, id: Int) {
        item.openIn(tabOpener = tabOpener, id = id)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun createImmutableTableChild(
        tabConfig: MainTabConfig.ItemListConfig,
        context: ComponentContext
    ): ImmutableTableChild {

        val immutableTableBuilderData: ImmutableTableBuilderData<out IMultiLineTableUi> =
            when (tabConfig) {
                is DeclarationListConfig -> DeclarationImmutableTableBuilder(withCheckbox = true)
                is DocumentListConfig -> DocumentImmutableTableBuilder(
                    fullListDocumentTypes = DocumentType.entries,
                    withCheckbox = true
                )

                is ProductListConfig -> ProductImmutableTableBuilder(
                    fullListProductTypes = ProductType.entries,
                    withCheckbox = true
                )

                is TransactionListConfig -> TransactionImmutableTableBuilder(
                    fullListTransactionTypes = TransactionType.entries,
                    withCheckbox = true
                )

                is VendorListConfig -> VendorImmutableTableBuilder(
                    withCheckbox = true
                )

                is ExpenseListConfig -> ExpenseImmutableTableBuilder(
                    withCheckbox = true
                )

                is SafetyListConfig -> SafetyImmutableTableBuilder()
            }


        fun <I : IMultiLineTableUi> onItemClick(item: I) = when (tabConfig) {
            is SafetyListConfig -> tabOpener.openProductTab(item.composeId)
            is DeclarationListConfig -> tabOpener.openDeclarationTab(item.composeId)
            is DocumentListConfig -> tabOpener.openDocumentTab(item.composeId)
            is ProductListConfig -> tabOpener.openProductTab(item.composeId)
            is VendorListConfig -> tabOpener.openVendorTab(item.composeId)
            is TransactionListConfig -> tabOpener.openTransactionTab(item.composeId)
            is ExpenseListConfig -> {
                val transactionId = (item as ExpenseTableUi).transactionId
                if (transactionId != null) {
                    tabOpener.openTransactionTab(transactionId)
                } else
                    tabOpener.openExpenseFormTab(
                        item.composeId
                    )
            }
        }

        return ImmutableTableChild(
            ImmutableTableComponentFactoryMain(
                componentContext = context,
                dependencies = scope.get(),
                onItemClick = { onItemClick(it) },
                immutableTableBuilderData = immutableTableBuilderData,
                tabOpener = tabOpener
            )
        )
    }


    private fun createItemFormChild(
        tabConfig: MainTabConfig.ItemFormConfig,
        context: ComponentContext
    ): MainTabChild.ItemFormChild {
        return when (tabConfig) {
            is DeclarationFormConfig -> DeclarationFormChild(
                DeclarationFormComponent(
                    declarationId = tabConfig.id,
                    componentContext = context,
                    dependencies = scope.get(),
                    tabOpener = tabOpener,
                )
            )

            is DocumentFormConfig -> DocumentFormChild(
                DocumentFormComponent(
                    documentId = tabConfig.id,
                    componentContext = context,
                    dependencies = scope.get()
                )
            )

            is ProductFormConfig -> ProductFormChild(
                ProductFormComponent(
                    componentContext = context,
                    dependencies = scope.get(),
                    productId = tabConfig.id,
                    tabOpener = tabOpener
                )
            )

            is TransactionFormConfig -> TransactionFormChild(
                TransactionFormComponent(
                    transactionId = tabConfig.id,
                    componentContext = context,
                    dependencies = scope.get(),
                    tabOpener = tabOpener
                )
            )

            is VendorFormConfig -> VendorFormChild(
                VendorFormComponent(
                    vendorId = tabConfig.id,
                    componentContext = context,
                    dependencies = scope.get()
                )
            )

            is ExpenseFormConfig -> ExpenseFormChild(
                ExpenseFormComponent(
                    componentContext = context,
                    dependencies = scope.get(),
                    expenseId = tabConfig.id
                )
            )
        }
    }


}



