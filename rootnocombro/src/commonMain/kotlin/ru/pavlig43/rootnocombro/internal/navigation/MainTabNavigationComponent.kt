package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.declaration.api.DeclarationFormComponent
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.immutable.api.component.DeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.DocumentImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ImmutableTableBuilderData
import ru.pavlig43.immutable.api.component.ImmutableTableComponentFactoryMain
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.api.component.TransactionImmutableTableBuilder
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ImmutableTableChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DeclarationFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DocumentFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.ProductFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.TransactionFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.VendorFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.NotificationChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.SampleTableChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DeclarationFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DocumentFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.ProductFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.TransactionFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.VendorFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DeclarationListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.DocumentListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.ProductListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.TransactionListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemListConfig.VendorListConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.NotificationConfig
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.sampletable.api.component.SampleTableComponentMain
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.vendor.component.VendorFormComponent

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
        val mainTabConfiguration: MainTabConfig = destination.toTabConfig()
        tabNavigationComponent.addTab(mainTabConfiguration)
    }

    private fun DrawerDestination.toTabConfig(): MainTabConfig =
        when (this) {
            DrawerDestination.DocumentList -> DocumentListConfig()
            DrawerDestination.ProductList -> ProductListConfig()
            DrawerDestination.VendorList -> VendorListConfig()
            DrawerDestination.DeclarationList -> DeclarationListConfig()
            DrawerDestination.ProductTransactionList -> TransactionListConfig()
            DrawerDestination.SampleTable -> MainTabConfig.SampleTableConfig()
        }

    val tabNavigationComponent: TabNavigationComponent<MainTabConfig, MainTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentListConfig(),
            ),
            serializer = MainTabConfig.serializer(),
            tabChildFactory = { context, mainTabConfig: MainTabConfig, onCloseTab: () -> Unit ->

                when (mainTabConfig) {
                    is NotificationConfig -> NotificationChild(notificationComponent)

                    is MainTabConfig.SampleTableConfig -> SampleTableChild(
                        SampleTableComponentMain(
                            componentContext = context
                        )
                    )

                    is MainTabConfig.ItemListConfig -> createImmutableTableChild(
                        tabConfig = mainTabConfig,
                        context = context
                    )


                    is MainTabConfig.ItemFormConfig -> createItemFormChild(
                        tabConfig = mainTabConfig,
                        context = context,
                        onCloseTab = onCloseTab
                    )

                }
            },
        )
    private val tabOpener = object : TabOpener {
        override fun openDocumentTab(id: Int) {
            tabNavigationComponent.addTab(DocumentFormConfig(id))
        }

        override fun openProductTab(id: Int) {
            tabNavigationComponent.addTab(ProductFormConfig(id))
        }

        override fun openVendorTab(id: Int) {
            tabNavigationComponent.addTab(VendorFormConfig(id))
        }

        override fun openDeclarationTab(id: Int) {
            tabNavigationComponent.addTab(DeclarationFormConfig(id))
        }

        override fun openTransactionTab(id: Int) {
            tabNavigationComponent.addTab(TransactionFormConfig(id))
        }

    }

    private fun openTabFromNotification(item: NotificationItem, id: Int) {
        when (item) {
            NotificationItem.Document -> tabOpener.openDocumentTab(id)
            NotificationItem.Product -> tabOpener.openProductTab(id)
            NotificationItem.Declaration -> tabOpener.openDeclarationTab(id)
            NotificationItem.Transaction -> tabOpener.openTransactionTab(id)
        }
    }

    private fun createImmutableTableChild(
        tabConfig: MainTabConfig.ItemListConfig,
        context: ComponentContext
    ): ImmutableTableChild {

        val immutableTableBuilderData: ImmutableTableBuilderData<out ITableUi> = when (tabConfig) {
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
        }

        fun formConfig(id: Int) = when (tabConfig) {
            is DeclarationListConfig -> DeclarationFormConfig(id)
            is DocumentListConfig -> DocumentFormConfig(id)
            is ProductListConfig -> ProductFormConfig(id)
            is VendorListConfig -> VendorFormConfig(id)
            is TransactionListConfig -> TransactionFormConfig(id)
        }
        return ImmutableTableChild(
            ImmutableTableComponentFactoryMain(
                componentContext = context,
                dependencies = scope.get(),
                onCreate = { tabNavigationComponent.addTab(formConfig(0)) },
                onItemClick = { tabNavigationComponent.addTab(formConfig(it.composeId)) },
                immutableTableBuilderData = immutableTableBuilderData
            )
        )
    }


    private fun createItemFormChild(
        tabConfig: MainTabConfig.ItemFormConfig,
        context: ComponentContext,
        onCloseTab: () -> Unit
    ): MainTabChild.ItemFormChild {
        return when (tabConfig) {
            is DeclarationFormConfig -> DeclarationFormChild(
                DeclarationFormComponent(
                    declarationId = tabConfig.id,
                    closeTab = onCloseTab,
                    componentContext = context,
                    dependencies = scope.get(),
                    tabOpener = tabOpener,
                )
            )

            is DocumentFormConfig -> DocumentFormChild(
                DocumentFormComponent(
                    documentId = tabConfig.id,
                    closeTab = onCloseTab,
                    componentContext = context,
                    dependencies = scope.get()
                )
            )

            is ProductFormConfig -> ProductFormChild(
                ProductFormComponent(
                    componentContext = context,
                    dependencies = scope.get(),
                    closeTab = onCloseTab,
                    productId = tabConfig.id,
                    tabOpener = tabOpener
                )
            )

            is TransactionFormConfig -> TransactionFormChild(
                TransactionFormComponent(
                    transactionId = tabConfig.id,
                    closeTab = onCloseTab,
                    componentContext = context,
                    dependencies = scope.get(),
                    tabOpener = tabOpener
                )
            )

            is VendorFormConfig -> VendorFormChild(
                VendorFormComponent(
                    vendorId = tabConfig.id,
                    closeTab = onCloseTab,
                    componentContext = context,
                    dependencies = scope.get()
                )
            )
        }
    }
}



