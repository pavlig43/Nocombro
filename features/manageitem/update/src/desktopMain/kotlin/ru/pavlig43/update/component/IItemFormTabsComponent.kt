package ru.pavlig43.update.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
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

/**
 * Интерфейс компонента управления вкладками формы редактирования объекта.
 *
 * Объединяет навигацию по вкладкам, логику обновления данных и выполнение транзакционных операций
 * для сложных форм с разделением на несколько вкладок/этапов.

 *
 * @property tabNavigationComponent Компонент навигации между вкладками формы
 * @property updateComponent Компонент управления процессом обновления данных
 * @property transactionExecutor Исполнитель транзакционных операций для атомарного сохранения данных
 *
 * @see TabNavigationComponent Компонент навигации по вкладкам
 * @see FormTabChild Базовый интерфейс компонента вкладки формы
 * @see UpdateComponent Компонент управления обновлением данных
 * @see TransactionExecutor Исполнитель транзакционных операций
 */
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
        val allFormChild = tabNavigationComponent.tabChildren.map { children ->
            children.items.map { childCreated -> childCreated.instance.component }
        }
        val blocks = allFormChild.map { value ->
            value.map { tab ->
                suspend { tab.onUpdate() }
            }
        }
        val result = transactionExecutor.transaction(blocks.value)
        if (result.isSuccess) {
            allFormChild.value.forEach {
                it.refreshDataAfterUpsert()
            }
        }
        return result
    }
}

fun IItemFormTabsComponent<*, *>.getDefaultUpdateComponent(
    componentContext: ComponentContext,
    postProcessAfterUpdate: suspend () -> Unit = {},
): UpdateComponent {
    return UpdateComponent(
        componentContext = componentContext.childContext("update"),
        onUpdateAllTabs = { update() },
        errorMessages = getErrors(componentContext.lifecycle),
        postProcessAfterUpdate = postProcessAfterUpdate
    )
}
