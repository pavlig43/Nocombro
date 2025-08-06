package ru.pavlig43.rootnocombro.internal.navigation.tab.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentlist.api.component.DocumentListComponent
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.productlist.api.component.ProductListComponent
import ru.pavlig43.rootnocombro.api.IRootDependencies
import ru.pavlig43.rootnocombro.internal.di.createRootNocombroModule
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.IDrawerComponent

interface IMainNavigationComponent<TabConfiguration : Any, SlotConfiguration : Any> {
    val drawerComponent: IDrawerComponent
    val tabNavigationComponent: ITabNavigationComponent<TabConfiguration, SlotConfiguration>
}

internal class MainNavigationComponent(
    componentContext: ComponentContext,
    rootDependencies: IRootDependencies
) : ComponentContext by componentContext, IMainNavigationComponent<TabConfig, SlotComponent> {

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createRootNocombroModule(
            rootDependencies
        )
    )
    override val drawerComponent: IDrawerComponent = DrawerComponent(
        componentContext = childContext("drawer"),
        openScreen = ::openScreenFromDrawer
    )

    fun openScreenFromDrawer(destination: DrawerDestination) {
        val tabConfiguration = destination.toTabConfig()
        tabNavigationComponent.addTab(tabConfiguration)
    }

    private fun DrawerDestination.toTabConfig(): TabConfig =
        when (this) {
            is DrawerDestination.CreateDocument -> TabConfig.CreateDocument()
            is DrawerDestination.Documents -> TabConfig.DocumentList()
            is DrawerDestination.ProductForm -> TabConfig.ProductForm(0)
            is DrawerDestination.ProductList -> TabConfig.ProductList()
        }

    override val tabNavigationComponent: ITabNavigationComponent<TabConfig, SlotComponent> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                TabConfig.DocumentList()
            ),
            serializer = TabConfig.serializer(),
            slotFactory = { context, tabConfig: TabConfig, openNewTab: (TabConfig) -> Unit, onCloseTab: () -> Unit ->

                when (tabConfig) {
                    is TabConfig.DocumentList -> DocumentListComponent(
                        componentContext = context,
                        onCreateScreen = { openNewTab(TabConfig.CreateDocument()) },
                        dependencies = scope.get(),
                        onItemClick = { openNewTab(TabConfig.ChangeDocument(it)) }
                    )

                    is TabConfig.CreateDocument -> DocumentFormComponent(
                        documentId = 0,
                        closeTab = onCloseTab,
                        componentContext = context,
                        dependencies = scope.get()
                    )

                    is TabConfig.ChangeDocument -> DocumentFormComponent(
                        componentContext = context,
                        dependencies = scope.get(),
                        closeTab = onCloseTab,
                        documentId = tabConfig.id
                    )

                    is TabConfig.ProductForm -> ProductFormComponent(
                        componentContext = context,
                        dependencies = scope.get(),
                        closeTab = onCloseTab,
                        productId = tabConfig.id
                    )

                    is TabConfig.ProductList -> ProductListComponent(
                        componentContext = context,
                        onItemClick = { openNewTab(TabConfig.ProductForm(it)) },
                        onCreateScreen = { openNewTab(TabConfig.ProductForm(0)) },
                        dependencies = scope.get()
                    )
                }
            },
        )


}


