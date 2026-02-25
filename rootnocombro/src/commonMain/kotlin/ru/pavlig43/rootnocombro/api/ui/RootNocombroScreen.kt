package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.layout.Box
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
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.coreui.tab.TabLazyRowNavigationContent
import ru.pavlig43.declaration.api.DeclarationFormScreen
import ru.pavlig43.document.api.ui.DocumentFormScreen
import ru.pavlig43.immutable.api.ui.ImmutableTableScreen
import ru.pavlig43.notification.api.ui.NotificationTabs
import ru.pavlig43.product.api.ui.ProductFormScreen
import ru.pavlig43.rootnocombro.api.component.RootChild
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import ru.pavlig43.rootnocombro.internal.navigation.MainTabChild
import ru.pavlig43.rootnocombro.internal.navigation.MainTabConfig
import ru.pavlig43.rootnocombro.internal.navigation.MainTabNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.drawer.ui.NavigationDrawer
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabContent
import ru.pavlig43.rootnocombro.internal.topbar.ui.NocombroAppBar
import ru.pavlig43.sampletable.api.ui.SampleTableScreen
import ru.pavlig43.signroot.api.ui.RootSignScreen
import ru.pavlig43.storage.api.StorageScreen
import ru.pavlig43.transaction.api.ui.TransactionFormScreen
import ru.pavlig43.vendor.api.ui.VendorFormScreen


@Suppress("LongMethod")
@Composable
fun RootNocombroScreen(rootNocombroComponent: RootNocombroComponent) {
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
        ) { child: Child.Created<RootNocombroComponent.RootConfig, RootChild> ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                when (val instance = child.instance) {
                    is RootChild.RootSign -> RootSignScreen(instance.component)

                    is RootChild.Tabs -> {
                        val mainTabNavigationComponent: MainTabNavigationComponent =
                            instance.component
                        val tabNavigationComponent: TabNavigationComponent<MainTabConfig, MainTabChild> =
                            mainTabNavigationComponent.tabNavigationComponent
                        val drawerNavigationComponent = mainTabNavigationComponent.drawerComponent
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
                                TabLazyRowNavigationContent(
                                    navigationComponent = tabNavigationComponent,
                                    tabContent = { index, mainTabChild, modifier, isSelected, isDragging, onClose ->
                                        TabContent(
                                            mainTabComponent = mainTabChild.component,
                                            modifier = modifier,
                                            isSelected = isSelected,
                                            isDragging = isDragging,
                                            onClose = onClose,
                                            onSelect = { tabNavigationComponent.onSelectTab(index) },
                                        )
                                    },
                                    tabChildFactory = { mainTabChild -> MainTabChildFactory(mainTabChild) }
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
private fun MainTabChildFactory(mainTabChild: MainTabChild?) {
    when (mainTabChild) {

        null -> Box(Modifier.fillMaxSize())
        is MainTabChild.ImmutableTableChild ->
            ImmutableTableScreen(mainTabChild.component)

        is MainTabChild.ItemFormChild.DeclarationFormChild ->
            DeclarationFormScreen(mainTabChild.component)

        is MainTabChild.ItemFormChild.DocumentFormChild ->
            DocumentFormScreen(mainTabChild.component)

        is MainTabChild.ItemFormChild.ProductFormChild ->
            ProductFormScreen(mainTabChild.component)

        is MainTabChild.ItemFormChild.TransactionFormChild ->
            TransactionFormScreen(mainTabChild.component)

        is MainTabChild.ItemFormChild.VendorFormChild ->
            VendorFormScreen(mainTabChild.component)

        is MainTabChild.NotificationChild ->
            NotificationTabs(mainTabChild.component)

        is MainTabChild.SampleTableChild -> SampleTableScreen(mainTabChild.component)

        is MainTabChild.StorageChild -> StorageScreen(mainTabChild.component)
    }
}




