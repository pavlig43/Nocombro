package ru.pavlig43.update.component

import com.arkivanov.decompose.value.operator.map
import com.arkivanov.essenty.lifecycle.Lifecycle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.FormTabChild
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.toStateFlow

interface IItemFormTabsComponent<TabConfiguration : Any, TabChild : FormTabChild> {
    val tabNavigationComponent: TabNavigationComponent<TabConfiguration, TabChild>
    val updateComponent: UpdateComponent

    val transactionExecutor: TransactionExecutor



    @OptIn(ExperimentalCoroutinesApi::class)
    fun getErrors(lifecycle: Lifecycle): Flow<List<ErrorMessage>> =
        tabNavigationComponent.tabChildren.map { children ->
            children.items.mapIndexed { tabIndex, child ->
                child.instance.component.errorMessages.map { errors ->
                    errors.map { messageText ->
                        ErrorMessage(
                            message = "На вкладке ${child.instance.component.title} $messageText",
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
    suspend fun update(): Result<Unit> {
        val blocks =
            tabNavigationComponent.tabChildren.map { children ->
                children.items.map { child -> suspend { child.instance.component.onUpdate() } }
            }
        return transactionExecutor.transaction(blocks.value)
    }
}

