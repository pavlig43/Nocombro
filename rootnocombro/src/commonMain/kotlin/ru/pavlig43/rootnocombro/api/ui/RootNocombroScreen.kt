package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.layout.*
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
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.coreui.tab.TabNavigationContent
import ru.pavlig43.declarationform.api.DeclarationFormComponent
import ru.pavlig43.declarationform.api.DeclarationFormScreen
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentform.api.ui.DocumentFormScreen
import ru.pavlig43.itemlist.api.component.ItemListFactoryComponent
import ru.pavlig43.itemlist.api.ui.GeneralItemListScreen
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.ui.NotificationTabs
import ru.pavlig43.productform.api.component.ProductFormComponent
import ru.pavlig43.productform.api.ui.ProductFormScreen
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.internal.navigation.IMainNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.ui.NavigationDrawer
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabContent
import ru.pavlig43.rootnocombro.internal.topbar.ui.NocombroAppBar
import ru.pavlig43.signroot.api.ui.RootSignScreen
import ru.pavlig43.vendor.component.VendorFormComponent
import ru.pavlig43.vendor.api.ui.VendorFormScreen

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
                        val tabNavigationComponent: TabNavigationComponent<TabConfig, SlotComponent> =
                            mainNavigationComponent.tabNavigationComponent
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

                                            is ItemListFactoryComponent -> GeneralItemListScreen(slotComponent)

                                            is DocumentFormComponent -> DocumentFormScreen(slotComponent)

                                            is ProductFormComponent -> ProductFormScreen(slotComponent)

                                            is PageNotificationComponent -> NotificationTabs(slotComponent)

                                            is VendorFormComponent -> VendorFormScreen(slotComponent)

                                            is DeclarationFormComponent -> DeclarationFormScreen(slotComponent)

                                            null -> Box(Modifier.fillMaxSize())
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




