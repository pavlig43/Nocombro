package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.declarationform.api.DeclarationFormComponent
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.itemlist.core.refac.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.core.refac.api.DocumentBuilder
import ru.pavlig43.itemlist.core.refac.api.DocumentListParamProvider
import ru.pavlig43.itemlist.core.refac.api.BuilderData
import ru.pavlig43.itemlist.core.refac.api.ProductListParamProvider
import ru.pavlig43.itemlist.core.refac.api.TransactionListParamProvider
import ru.pavlig43.itemlist.core.refac.api.VendorListParamProvider
import ru.pavlig43.itemlist.core.refac.core.component.ImmutableTableBuilder
import ru.pavlig43.itemlist.core.refac.core.component.ImmutableTableComponent
import ru.pavlig43.itemlist.statik.api.component.StaticItemListFactoryComponent
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.IDrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm.DeclarationForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm.DocumentForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm.ProductForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm.TransactionForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemForm.VendorForm
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList.DeclarationList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList.DocumentList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList.ProductList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList.ProductTransactionList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.ItemList.VendorList
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig.Notification
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.vendor.component.VendorFormComponent

interface IMainNavigationComponent<TabConfiguration : Any, SlotConfiguration : Any> {
    val drawerComponent: IDrawerComponent
    val tabNavigationComponent: TabNavigationComponent<TabConfiguration, SlotConfiguration>
}

internal class MainNavigationComponent(
    componentContext: ComponentContext,
    private val scope: Scope,
) : ComponentContext by componentContext, IMainNavigationComponent<TabConfig, SlotComponent> {

    private val notificationComponent = PageNotificationComponent(
        componentContext = childContext("notification"),
        onOpenTab = ::openTabFromNotification,
        dependencies = scope.get()
    )

    override val drawerComponent: IDrawerComponent = DrawerComponent(
        componentContext = childContext("drawer"),
        openScreen = ::openScreenFromDrawer,
        onNotificationScreen = { tabNavigationComponent.addTab(Notification()) },
        notificationsState = notificationComponent.notificationsForDrawer
    )

    fun openScreenFromDrawer(destination: DrawerDestination) {
        val tabConfiguration: TabConfig = destination.toTabConfig()
        tabNavigationComponent.addTab(tabConfiguration)
    }

    private fun DrawerDestination.toTabConfig(): TabConfig =
        when (this) {
            DrawerDestination.DocumentList -> DocumentList()
            DrawerDestination.ProductList -> ProductList()
            DrawerDestination.VendorList -> VendorList()
            DrawerDestination.DeclarationList -> DeclarationList()
            DrawerDestination.ProductTransactionList -> ProductTransactionList()
        }

    override val tabNavigationComponent: TabNavigationComponent<TabConfig, SlotComponent> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DocumentList(),
            ),
            serializer = TabConfig.serializer(),
            slotFactory = { context, tabConfig: TabConfig, onCloseTab: () -> Unit ->

                when (tabConfig) {
                    is Notification -> notificationComponent
                    is DocumentList-> ImmutableTableBuilder.build(
                        context = context,
                        dependencies = scope.get(),
                        onCreate = { tabNavigationComponent.addTab(DocumentForm(0)) },
                        onItemClick = { tabNavigationComponent.addTab(DocumentForm(it.id)) },
                        builderData = DocumentBuilder(
                            fullListDocumentTypes = DocumentType.entries,
                            withCheckbox = true
                        )
                    )
                    is ItemList -> createImmutableTableComponent(
                        tabConfig = tabConfig,
                        context = context
                    )
                    is ItemList -> createItemListFactoryComponent(
                        tabConfig = tabConfig,
                        context = context
                    )

                    is ItemForm -> createItemFormComponent(
                        tabConfig = tabConfig,
                        context = context,
                        onCloseTab = onCloseTab
                    )

                }
            },
        )

    private fun openTabFromNotification(item: NotificationItem, id: Int) {
        val tab = when (item) {
            NotificationItem.Document -> DocumentForm(id)
            NotificationItem.Product -> ProductForm(id)
            NotificationItem.Declaration -> DeclarationForm(id)
        }
        tabNavigationComponent.addTab(tab)
    }
    private fun createImmutableTableComponent(
        tabConfig: ItemList,
        context: ComponentContext
    ): ImmutableTableComponent<*,*,*>{
        val a= DocumentBuilder(
            fullListDocumentTypes = DocumentType.entries,
            withCheckbox = true
        )
        val builderData: BuilderData<*> = when(tabConfig){
            is DeclarationList -> a
            is DocumentList -> a
            is ProductList -> a
            is ProductTransactionList -> a
            is VendorList -> a
        }
        fun itemForm(id: Int) = when (tabConfig) {
            is DeclarationList -> DeclarationForm(id)
            is DocumentList -> DocumentForm(id)
            is ProductList -> ProductForm(id)
            is VendorList -> VendorForm(id)
            is ProductTransactionList -> TransactionForm(id)
        }
        return ImmutableTableBuilder.build(
            context = context,
            dependencies = scope.get(),
            onCreate = { tabNavigationComponent.addTab(itemForm(0)) },
            onItemClick = { tabNavigationComponent.addTab(itemForm(it.id)) },
            builderData = builderData
        )
    }


    private fun createItemListFactoryComponent(
        tabConfig: ItemList,
        context: ComponentContext

    ): StaticItemListFactoryComponent {
        val paramProvider = when (tabConfig) {
            is DeclarationList -> DeclarationListParamProvider(withCheckbox = true)
            is DocumentList -> DocumentListParamProvider(
                fullListDocumentTypes = DocumentType.entries,
                withCheckbox = true
            )

            is ProductList -> ProductListParamProvider(
                fullListProductTypes = ProductType.entries,
                withCheckbox = true
            )

            is VendorList -> VendorListParamProvider(withCheckbox = true)
            is ProductTransactionList -> TransactionListParamProvider(
                fullListTransactionTypes = TransactionType.entries,
                withCheckbox = true
            )
        }

        fun itemForm(id: Int) = when (tabConfig) {
            is DeclarationList -> DeclarationForm(id)
            is DocumentList -> DocumentForm(id)
            is ProductList -> ProductForm(id)
            is VendorList -> VendorForm(id)
            is ProductTransactionList -> TransactionForm(id)
        }
        return StaticItemListFactoryComponent(
            componentContext = context,
            itemStaticListDependencies = scope.get(),
            onCreate = { tabNavigationComponent.addTab(itemForm(0)) },
            onItemClick = { tabNavigationComponent.addTab(itemForm(it.id)) },
            immutableTableBuilder = paramProvider
        )

    }
    private fun createItemFormComponent(
        tabConfig: ItemForm,
        context: ComponentContext,
        onCloseTab: () -> Unit
    ): SlotComponent {
        return when(tabConfig){
            is DeclarationForm -> DeclarationFormComponent(
                declarationId = tabConfig.id,
                closeTab = onCloseTab,
                componentContext = context,
                dependencies = scope.get(),
                onOpenVendorTab = { tabNavigationComponent.addTab(VendorForm(it)) }
            )
            is DocumentForm -> DocumentFormComponent(
                documentId = tabConfig.id,
                closeTab = onCloseTab,
                componentContext = context,
                dependencies = scope.get()
            )
            is ProductForm -> ProductFormComponent(
                componentContext = context,
                dependencies = scope.get(),
                closeTab = onCloseTab,
                productId = tabConfig.id,
                onOpenDeclarationTab = { tabNavigationComponent.addTab(DeclarationForm(it)) },
                onOpenProductTab = { tabNavigationComponent.addTab(ProductForm(it)) }
            )
            is TransactionForm -> TransactionFormComponent(
                transactionId = tabConfig.id,
                closeTab = onCloseTab,
                componentContext = context,
                dependencies = scope.get()
            )
            is VendorForm -> VendorFormComponent(
                vendorId = tabConfig.id,
                closeTab = onCloseTab,
                componentContext = context,
                dependencies = scope.get()
            )
        }
    }
}



