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
import ru.pavlig43.document.api.component.DocumentFormComponent
import ru.pavlig43.document.api.ui.DocumentFormScreen
import ru.pavlig43.itemlist.core.refac.core.component.ImmutableTableComponent
import ru.pavlig43.itemlist.core.refac.core.ui.ImmutableListBox
import ru.pavlig43.itemlist.statik.api.component.StaticItemListFactoryComponent
import ru.pavlig43.itemlist.statik.api.ui.GeneralItemListScreen
import ru.pavlig43.notification.api.component.PageNotificationComponent
import ru.pavlig43.notification.api.ui.NotificationTabs
import ru.pavlig43.product.api.component.ProductFormComponent
import ru.pavlig43.product.api.ui.ProductFormScreen
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.internal.navigation.IMainNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.ui.NavigationDrawer
import ru.pavlig43.rootnocombro.internal.navigation.tab.TabConfig
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabContent
import ru.pavlig43.rootnocombro.internal.topbar.ui.NocombroAppBar
import ru.pavlig43.signroot.api.ui.RootSignScreen
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.transaction.api.ui.TransactionFormScreen
import ru.pavlig43.vendor.api.ui.VendorFormScreen
import ru.pavlig43.vendor.component.VendorFormComponent


@Suppress("LongMethod")
@Composable
fun RootNocombroScreen(rootNocombroComponent: IRootNocombroComponent) {
    val stack by rootNocombroComponent.stack.subscribeAsState()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)


//    SampleApp()
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
                                    containerContent = { innerTabs: @Composable (modifier: Modifier) -> Unit ->
                                        innerTabs(Modifier.fillMaxWidth())

                                    },
                                    slotFactory = {slotComponent -> SlotFactory(slotComponent)}
                                )
                            }
                        }


                    }
                }

            }
        }
    }

}
@Composable
private fun SlotFactory(slotComponent: SlotComponent?){
    when (slotComponent) {
        is ImmutableTableComponent<*, *, *> -> ImmutableListBox(slotComponent)

        is StaticItemListFactoryComponent -> GeneralItemListScreen(slotComponent)

        is DocumentFormComponent -> DocumentFormScreen(slotComponent)

        is ProductFormComponent -> ProductFormScreen(slotComponent)

        is PageNotificationComponent -> NotificationTabs(slotComponent)

        is VendorFormComponent -> VendorFormScreen(slotComponent)

        is DeclarationFormComponent -> DeclarationFormScreen(slotComponent)

        is TransactionFormComponent -> TransactionFormScreen(slotComponent)

        null -> Box(Modifier.fillMaxSize())
        else -> error("$slotComponent SlotComponent not added")
    }
}




