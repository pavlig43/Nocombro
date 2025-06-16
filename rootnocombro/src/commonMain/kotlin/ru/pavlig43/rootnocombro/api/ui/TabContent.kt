package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.rootnocombro.api.component.TabNavigationComponent

@Composable
internal fun <TabComponent : Any> TabContent(
    navigationComponent: TabNavigationComponent<TabComponent>,
    tabContent: @Composable (
        index: Int,
        tabComponent: TabComponent,
        modifier: Modifier,
        isSelected: Boolean,
        isDragging: Boolean,
        onTabCloseClicked: () -> Unit,
    ) -> Unit,
    tabsArrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp),
    containerContent: @Composable (innerTabs: @Composable (modifier: Modifier) -> Unit, tabComponent: TabComponent?) -> Unit
) {
    val children by navigationComponent.children.subscribeAsState()
    containerContent(
        { tabRowModifier ->
            TabDraggableRow(
                items = children.items.map { it.instance },
                onMove = navigationComponent::onMove,
                modifier = tabRowModifier,
                tabHorizontalSpacing = tabsArrangement,
                itemContent = { index, tabComponent, isDragging, modifier ->
                    tabContent(
                        index,
                        tabComponent,
                        modifier,
                        children.selected == index,
                        isDragging,
                    ) {
                        navigationComponent.onTabCloseClicked(index)
                    }
                }

            )
        },
        children.selected?.let { children.items[it].instance }
    )

}