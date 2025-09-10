package ru.pavlig43.rootnocombro.internal.navigation

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.itemlist.api.component.ItemListComponent
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.data.NotificationItem
//import ru.pavlig43.notification.api.component.NotificationComponent
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.rootnocombro.internal.di.ItemListType
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.IDrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig

interface IMainNavigationComponent<TabConfiguration : Any, SlotConfiguration : Any> {
    val drawerComponent: IDrawerComponent
    val tabNavigationComponent: ITabNavigationComponent<TabConfiguration, SlotConfiguration>
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
            DrawerDestination.CreateDocument -> TabConfig.DocumentForm(0)
            DrawerDestination.Documents -> TabConfig.DocumentList()
            DrawerDestination.ProductForm -> TabConfig.ProductForm(0)
            DrawerDestination.ProductList -> TabConfig.ProductList()
        }

    override val tabNavigationComponent: ITabNavigationComponent<TabConfig, SlotComponent> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                TabConfig.ProductList(),
            ),
            serializer = TabConfig.serializer(),
            slotFactory = { context, tabConfig: TabConfig, openNewTab: (TabConfig) -> Unit, onCloseTab: () -> Unit ->

                when (tabConfig) {
                    is TabConfig.DocumentList -> ItemListComponent<Document,DocumentType>(
                        componentContext = context,
                        tabTitle = "Документы",
                        onCreate = { openNewTab(TabConfig.DocumentForm(0)) },
                        repository = scope.get(named(ItemListType.Document.name)),
                        fullListSelection = DocumentType.entries,
                        onItemClick = {id,_-> openNewTab(TabConfig.DocumentForm(id)) },
                        withCheckbox = true
                    )
                    is TabConfig.ProductList -> ItemListComponent<Product,ProductType>(
                        componentContext = context,
                        tabTitle = "Продукты",
                        onItemClick = { id, _ -> openNewTab(TabConfig.ProductForm(id)) },
                        onCreate = { openNewTab(TabConfig.ProductForm(0)) },
                        repository = scope.get(named(ItemListType.Product.name)),
                        fullListSelection = ProductType.entries,
                        withCheckbox = true
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
                        onOpenDocumentTab = {openNewTab(TabConfig.DocumentForm(it))}
                    )

                    is TabConfig.Notification -> notificationComponent
                }
            },
        )

    private fun openTabFromNotification(item:NotificationItem,id:Int){
        when(item){
            NotificationItem.Document -> tabNavigationComponent.addTab(TabConfig.DocumentForm(id))
            NotificationItem.Product -> tabNavigationComponent.addTab(TabConfig.ProductForm(id))
        }
    }
}



