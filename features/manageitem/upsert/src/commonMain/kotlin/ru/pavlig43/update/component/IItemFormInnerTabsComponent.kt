package ru.pavlig43.update.component

import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.toStateFlow

interface IItemFormInnerTabsComponent<Tab : Any, SlotComponent : FormTabSlot> {
    val tabNavigationComponent: TabNavigationComponent<Tab, SlotComponent>
    val updateComponent: UpdateComponent

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getErrors(lifecycle: Lifecycle): Flow<List<ErrorMessage>> =
        tabNavigationComponent.tabChildren.map { children ->
            children.items.mapIndexed { tabIndex, child ->
                child.instance.errorMessages.map { errors ->
                    errors.map { messageText ->
                        ErrorMessage(
                            message = "На вкладке ${child.instance.title} $messageText",
                            onSelectProblemTab = { tabNavigationComponent.onSelectTab(tabIndex) }
                        )
                    }
                }
            }
        }.toStateFlow(lifecycle).flatMapLatest { flowList ->
            combine(flowList) {
                it.toList().flatten()
            }
        }
}

