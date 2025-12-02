package ru.pavlig43.coreui.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.tabs.TabNavigationComponent

@Composable
fun <TabConfiguration : Any, SlotComponent : Any> TabNavigationContent(
    navigationComponent: TabNavigationComponent<TabConfiguration, SlotComponent>,
    tabContent: @Composable (
        index: Int,
        slotComponent: SlotComponent,
        modifier: Modifier,
        isSelected: Boolean,
        isDragging: Boolean,
        onClose: () -> Unit
    ) -> Unit,
    tabsArrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp),
    containerContent: @Composable (
        innerTabs: @Composable (modifier: Modifier) -> Unit,
        slotComponent: SlotComponent?
    ) -> Unit
) {
    val children by navigationComponent.children.subscribeAsState()

    containerContent(
        { tabRowModifier ->
            TabDraggableRow<SlotComponent>(
                items = children.items.map { it.instance },
                onMove = navigationComponent::onMove,
                modifier = tabRowModifier,
                tabHorizontalSpacing = tabsArrangement,
                itemContent = { index, itemComponent: SlotComponent, isDragging, modifier ->
                    tabContent(
                        index,
                        itemComponent,
                        modifier,
                        children.selectedIndex == index,
                        isDragging
                    ) { navigationComponent.onTabCloseClicked(index) }
                }
            )
        },
        children.selectedIndex?.let { children.items[it] }?.instance
    )
}