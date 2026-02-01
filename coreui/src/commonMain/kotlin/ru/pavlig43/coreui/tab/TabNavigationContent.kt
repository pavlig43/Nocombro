package ru.pavlig43.coreui.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.InternalDecomposeApi
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.keyHashString
import ru.pavlig43.core.tabs.TabNavigationComponent

/**
 * Отрисовывет полностью экран с вкладками(как в браузере)
 * @param [tabContent] отрисовывает саму вкладку (которая с крестиком)
 * @param [tabChildFactory]отрисовывает экран, который под вкладкой
 */
@OptIn(InternalDecomposeApi::class)
@Composable
fun <TabConfiguration : Any, TabChild : Any> TabLazyRowNavigationContent(
    navigationComponent: TabNavigationComponent<TabConfiguration, TabChild>,
    tabContent: @Composable (
        index: Int,
        tabChild: TabChild,
        modifier: Modifier,
        isSelected: Boolean,
        isDragging: Boolean,
        onClose: () -> Unit
    ) -> Unit,
    tabsArrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp),
    tabsRowModifier: Modifier = Modifier,
    tabChildFactory: @Composable (TabChild?) -> Unit,
) {
    val children by navigationComponent.tabChildren.subscribeAsState()

    /**
     * Для того чтобы сохранять состояние скролла
     * @sample "https://github.com/arkivanov/Decompose/blob/master/extensions-compose/src/commonMain/kotlin/com/arkivanov/decompose/extensions/compose/stack/Children.kt?ysclid=mkgi5tjc2z853230913"
     */
    val holder = rememberSaveableStateHolder()
    holder.retainStates(children.getKeys())

    TabDraggableRow<TabChild>(
        items = children.items.map { it.instance },
        onMove = navigationComponent::onMove,
        modifier = tabsRowModifier.fillMaxWidth(),
        tabHorizontalSpacing = tabsArrangement,
        itemContent = { index, itemComponent: TabChild, isDragging, modifier ->
            tabContent(
                index,
                itemComponent,
                modifier,
                children.selectedIndex == index,
                isDragging
            ) { navigationComponent.onTabCloseClicked(index) }
        }
    )

    val activeTab = children.selectedIndex?.let { children.items[it] }
    val key = activeTab?.keyHashString()
    val instance = activeTab?.instance
    key?.let { key ->
        holder.SaveableStateProvider(key) {
            tabChildFactory(instance)
        }
    }
}

/**
 * @see [TabLazyRowNavigationContent]
 */
@OptIn(InternalDecomposeApi::class)
@Composable
fun <TabConfiguration : Any, TabChild : Any> TabStaticNavigationContent(
    navigationComponent: TabNavigationComponent<TabConfiguration, TabChild>,
    tabContent: @Composable (
        index: Int,
        tabChild: TabChild,
        isSelected: Boolean,
    ) -> Unit,
    tabsArrangement: Arrangement.Horizontal = Arrangement.spacedBy(4.dp),
    tabsRowModifier: Modifier = Modifier,
    tabChildFactory: @Composable (TabChild?) -> Unit,
) {
    val children by navigationComponent.tabChildren.subscribeAsState()

    /**
     * Для того чтобы сохранять состояние скролла
     * @sample "https://github.com/arkivanov/Decompose/blob/master/extensions-compose/src/commonMain/kotlin/com/arkivanov/decompose/extensions/compose/stack/Children.kt?ysclid=mkgi5tjc2z853230913"
     */
    val holder = rememberSaveableStateHolder()
    holder.retainStates(children.getKeys())
    Row(
        modifier = tabsRowModifier.fillMaxWidth(),
        horizontalArrangement = tabsArrangement,
        verticalAlignment = Alignment.CenterVertically
        ){
        children.items.map { it.instance }.forEachIndexed { index, child ->
            tabContent(
                index,
                child,
                children.selectedIndex == index
            )
        }
    }

    val activeTab = children.selectedIndex?.let { children.items[it] }
    val key = activeTab?.keyHashString()
    val instance = activeTab?.instance
    key?.let { key ->
        holder.SaveableStateProvider(key) {
            tabChildFactory(instance)
        }
    }
}


@Suppress("MaxLineLength")
@OptIn(InternalDecomposeApi::class)
private fun <TabConfiguration : Any, TabChild : Any> TabNavigationComponent.TabChildren<TabConfiguration, TabChild>.getKeys(): HashSet<String> {
    return items.mapTo(HashSet(), Child<*, *>::keyHashString)
}


@Suppress("ComposableNaming")
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