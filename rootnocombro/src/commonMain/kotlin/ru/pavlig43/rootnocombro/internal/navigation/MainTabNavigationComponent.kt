package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.declaration.api.DeclarationFormComponent
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.immutable.api.component.*
import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.notification.api.model.NotificationItem
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig.ItemFormConfig.DeclarationFormConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.NotificationChild
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
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.vendor.component.VendorFormComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DeclarationFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.DocumentFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.ProductFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.VendorFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ItemFormChild.TransactionFormChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild.ImmutableTableChild

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

    private fun openTabFromNotification(item: NotificationItem, id: Int) {
        val tab = when (item) {
            NotificationItem.Document -> DocumentFormConfig(id)
            NotificationItem.Product -> ProductFormConfig(id)
            NotificationItem.Declaration -> DeclarationFormConfig(id)
        }
        tabNavigationComponent.addTab(tab)
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
                    onOpenVendorTab = { tabNavigationComponent.addTab(VendorFormConfig(it)) }
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
                onOpenDeclarationTab = { tabNavigationComponent.addTab(DeclarationFormConfig(it)) },
                onOpenProductTab = { tabNavigationComponent.addTab(ProductFormConfig(it)) }
            ))

            is TransactionFormConfig -> TransactionFormChild(
                TransactionFormComponent(
                    transactionId = tabConfig.id,
                    closeTab = onCloseTab,
                    componentContext = context,
                    dependencies = scope.get()
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



