package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.coroutines.launch
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentform.api.ui.DocumentFormScreen
import ru.pavlig43.documentlist.api.component.DocumentListComponent
import ru.pavlig43.documentlist.api.ui.DocumentScreen
import ru.pavlig43.productlist.api.ui.ProductsScreen
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.productform.api.ui.ProductFormScreen
import ru.pavlig43.productlist.api.component.ProductListComponent
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.ui.NavigationDrawer
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.IMainNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabConfig
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabContent
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabNavigationContent
import ru.pavlig43.rootnocombro.internal.topbar.ui.NocombroAppBar
import ru.pavlig43.signroot.api.ui.RootSignScreen

@Suppress("LongMethod")
@Composable
fun RootNocombroScreen(rootNocombroComponent: IRootNocombroComponent) {
    val stack by rootNocombroComponent.stack.subscribeAsState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    Surface {

        Children(
            stack = stack,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.systemBars)
        ) { child: Child.Created<Any, IRootNocombroComponent.Child> ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                when (val instance = child.instance) {
                    is IRootNocombroComponent.Child.RootSign -> RootSignScreen(instance.component)

                    is IRootNocombroComponent.Child.Tabs -> {
                        val mainNavigationComponent: IMainNavigationComponent<TabConfig, SlotComponent> =
                            instance.component
                        val tabNavigationComponent: ITabNavigationComponent<TabConfig, SlotComponent> = mainNavigationComponent.tabNavigationComponent
                        val drawerNavigationComponent = mainNavigationComponent.drawerComponent
                        NocombroAppBar(
                            settingsComponent = rootNocombroComponent.settingsComponent,
                            onOpenDrawer = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    } else {
                                        drawerState.close()
                                    }
                                }
                            }
                        )
                        NavigationDrawer(
                            drawerComponent = drawerNavigationComponent,
                            drawerState = drawerState,
                            onCloseNavigationDrawer = {
                                coroutineScope.launch { drawerState.close() }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxSize()) {
                                TabNavigationContent(
                                    navigationComponent = tabNavigationComponent,
                                    tabContent = { index, slotComponent, modifier, isSelected, isDragging, onClose ->
                                        TabContent(
                                            slotComponent = slotComponent,
                                            modifier = modifier,
                                            isSelected = isSelected,
                                            isDragging = isDragging,
                                            onClose = onClose,
                                            onSelect = { tabNavigationComponent.onSelectTab(index) },
                                        )
                                    },
                                    containerContent = { innerTabs: @Composable (modifier: Modifier) -> Unit,
                                                         slotComponent: SlotComponent? ->
                                        innerTabs(Modifier.fillMaxWidth())
                                        when (slotComponent) {
                                            is DocumentListComponent -> DocumentScreen(slotComponent)
                                            is DocumentFormComponent -> DocumentFormScreen(slotComponent)
                                            is ProductFormComponent -> ProductFormScreen(slotComponent)
                                            is ProductListComponent -> ProductsScreen(slotComponent)
                                            else -> error("$slotComponent SlotComponent not added")
                                        }
                                    }
                                )
                            }

                        }


                    }
                }

            }
        }
    }

}




