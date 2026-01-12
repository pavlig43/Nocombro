package ru.pavlig43.coreui.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.keyHashString
import com.arkivanov.decompose.router.stack.ChildStack
import ru.pavlig43.core.tabs.TabNavigationComponent

@OptIn(InternalDecomposeApi::class)
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
    ) -> Unit,
    slotFactory: @Composable (SlotComponent?) -> Unit,
) {
    val children by navigationComponent.children.subscribeAsState()
    val holder = rememberSaveableStateHolder()
    holder.retainStates(children.getKeys())


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

        )
    val activeSlot = children.selectedIndex?.let { children.items[it] }
    val key = activeSlot?.keyHashString()
    val instance = activeSlot?.instance
        key?.let { key ->
            holder.SaveableStateProvider(key) {
                slotFactory(instance)
            }
        }
    }


@OptIn(InternalDecomposeApi::class)
private fun <TabConfiguration : Any, SlotComponent : Any> TabNavigationComponent.Children<TabConfiguration, SlotComponent>.getKeys(): HashSet<String> {
    return items.mapTo(HashSet(), Child<*, *>::keyHashString)
}


@Composable
private fun SaveableStateHolder.retainStates(currentKeys: Set<String>) {
    val keys = remember(this) { Keys(currentKeys) }

    DisposableEffect(this, currentKeys) {
        keys.set.forEach {
            if (it !in currentKeys) {
                removeState(it)
            }
        }

        keys.set = currentKeys

        onDispose {}
    }
}

private class Keys(
    var set: Set<Any>
)