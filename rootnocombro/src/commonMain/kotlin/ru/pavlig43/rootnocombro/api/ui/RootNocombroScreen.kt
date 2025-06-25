package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.document.api.component.CreateDocumentComponent
import ru.pavlig43.document.api.component.DocumentComponent
import ru.pavlig43.document.api.ui.CreateDocumentScreen
import ru.pavlig43.document.api.ui.DocumentScreen
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.internal.settings.component.ISettingsComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabConfig
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabContent
import ru.pavlig43.rootnocombro.internal.navigation.tab.ui.TabNavigationContent
import ru.pavlig43.rootnocombro.internal.topbar.ui.NocombroAppBar
import ru.pavlig43.signroot.api.ui.RootSignScreen

@Composable
fun RootNocombroScreen(rootNocombroComponent: IRootNocombroComponent) {
    val stack by rootNocombroComponent.stack.subscribeAsState()

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
                        val tabNavigationComponent: TabNavigationComponent<TabConfig, SlotComponent> =
                            instance.component
                        NocombroAppBar(settingsComponent = rootNocombroComponent.settingsComponent)
                        Column(modifier = Modifier.fillMaxSize()) {
                            TabNavigationContent(
                                navigationComponent = tabNavigationComponent,
                                tabContent = { index, slotComponent, modifier, isSelected, isDragging, onClose ->
                                    TabContent(
                                        slotComponent = slotComponent,
                                        modifier = modifier,
                                        isSelected = isSelected,
                                        isDragging = isDragging,
                                        onClose = onClose,
                                        onSelect = { tabNavigationComponent.onSelect(index) },
                                    )
                                },
                                containerContent = { innerTabs, slotComponent: SlotComponent? ->
                                    innerTabs(Modifier.fillMaxWidth())
                                    when (slotComponent) {
                                        is DocumentComponent -> DocumentScreen(slotComponent)
                                        is CreateDocumentComponent -> CreateDocumentScreen(slotComponent)
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




