package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.declarationform.api.DeclarationFormComponent
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.itemlist.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.DocumentListParamProvider
import ru.pavlig43.itemlist.api.ProductListParamProvider
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.component.ItemListFactoryComponent
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.data.NotificationItem
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.IDrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig
import ru.pavlig43.vendor.component.VendorFormComponent

interface IMainNavigationComponent<TabConfiguration : Any, SlotConfiguration : Any> {
    val drawerComponent: IDrawerComponent
    val tabNavigationComponent: TabNavigationComponent<TabConfiguration, SlotConfiguration>
}

internal class MainNavigationComponent(
    componentContext: ComponentContext,
    scope:Scope,
) : ComponentContext by componentContext, IMainNavigationComponent<TabConfig, SlotComponent> {

    private val notificationComponent = PageNotificationComponent(
        componentContext = childContext("notification"),
        onOpenTab = ::openTabFromNotification,
        dependencies = scope.get()
    )

    override val drawerComponent: IDrawerComponent = DrawerComponent(
        componentContext = childContext("drawer"),
        openScreen = ::openScreenFromDrawer,
        onNotificationScreen = {tabNavigationComponent.addTab(TabConfig.Notification())},
        notificationsState = notificationComponent.notificationsForDrawer
    )

    fun openScreenFromDrawer(destination: DrawerDestination) {
        val tabConfiguration: TabConfig = destination.toTabConfig()
        tabNavigationComponent.addTab(tabConfiguration)
    }

    private fun DrawerDestination.toTabConfig(): TabConfig =
        when (this) {
            DrawerDestination.DocumentList -> TabConfig.DocumentList()
            DrawerDestination.ProductList -> TabConfig.ProductList()
            DrawerDestination.VendorList -> TabConfig.VendorList()
            DrawerDestination.DeclarationList -> TabConfig.DeclarationList()
        }

    override val tabNavigationComponent: TabNavigationComponent<TabConfig, SlotComponent> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                TabConfig.ProductList(),
            ),
            serializer = TabConfig.serializer(),
            slotFactory = { context, tabConfig: TabConfig, openNewTab: (TabConfig) -> Unit, onCloseTab: () -> Unit ->

                when (tabConfig) {
                    is TabConfig.DocumentList -> ItemListFactoryComponent(
                        componentContext = context,
                        itemListDependencies = scope.get(),
                        onCreate = { openNewTab(TabConfig.DocumentForm(0)) },
                        onItemClick = { openNewTab(TabConfig.DocumentForm(it.id)) },
                        itemListParamProvider = DocumentListParamProvider(
                            fullListDocumentTypes = DocumentType.entries,
                            withCheckbox = true,
                        )
                    )

                    is TabConfig.ProductList -> ItemListFactoryComponent(
                        componentContext = context,
                        itemListDependencies = scope.get(),
                        onCreate = { openNewTab(TabConfig.ProductForm(0)) },
                        onItemClick = { openNewTab(TabConfig.ProductForm(it.id)) },
                        itemListParamProvider = ProductListParamProvider(
                            fullListProductTypes = ProductType.entries,
                            withCheckbox = true,
                        )
                    )
                    is TabConfig.DocumentForm -> DocumentFormComponent(
                        documentId = tabConfig.id,
                        closeTab = onCloseTab,
                        componentContext = context,
                        dependencies = scope.get()
                    )

                    is TabConfig.ProductForm -> ProductFormComponent(
                        componentContext = context,
                        dependencies = scope.get(),
                        closeTab = onCloseTab,
                        productId = tabConfig.id,
                        onOpenDeclarationTab = {openNewTab(TabConfig.DeclarationForm(it))},
                        onOpenProductTab = {openNewTab(TabConfig.ProductForm(it))}
                    )

                    is TabConfig.Notification -> notificationComponent
                    is TabConfig.VendorForm -> VendorFormComponent(
                        vendorId = tabConfig.id,
                        closeTab = onCloseTab,
                        componentContext = context,
                        dependencies = scope.get()
                    )
                    is TabConfig.VendorList -> ItemListFactoryComponent(
                        componentContext = context,
                        onCreate = { openNewTab(TabConfig.VendorForm(0)) },
                        onItemClick = { openNewTab(TabConfig.VendorForm(it.id)) },
                        itemListDependencies = scope.get(),
                        itemListParamProvider = VendorListParamProvider(
                            withCheckbox = true,
                        )
                    )

                    is TabConfig.DeclarationForm -> DeclarationFormComponent(
                        declarationId = tabConfig.id,
                        closeTab = onCloseTab,
                        componentContext = context,
                        dependencies = scope.get(),
                        onOpenVendorTab = {openNewTab(TabConfig.VendorForm(it))}
                    )
                    is TabConfig.DeclarationList -> ItemListFactoryComponent(
                        componentContext = context,
                        itemListDependencies = scope.get(),
                        onCreate = { openNewTab(TabConfig.DeclarationForm(0)) },
                        onItemClick = { openNewTab(TabConfig.DeclarationForm(it.id)) },
                        itemListParamProvider = DeclarationListParamProvider(
                            withCheckbox = true,
                        )
                    )
                }
            },
        )

    private fun openTabFromNotification(item:NotificationItem,id:Int){
        when(item){
            NotificationItem.Document -> tabNavigationComponent.addTab(TabConfig.DocumentForm(id))
            NotificationItem.Product -> tabNavigationComponent.addTab(TabConfig.ProductForm(id))
            NotificationItem.Declaration -> tabNavigationComponent.addTab(TabConfig.DeclarationForm(id))
        }
    }
}



