package ru.pavlig43.nocombro.mobile.experiments

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * Зависимости mobile-компонента экспериментов.
 */
class ExperimentsMobileDependencies(
    val repositoryFactory: (CoroutineScope) -> ExperimentsRepository,
)

/**
 * Decompose-компонент mobile-экрана экспериментов.
 *
 * Владеет scope экрана и отдаёт UI только state и команды.
 */
class ExperimentsMobileComponent(
    componentContext: ComponentContext,
    dependencies: ExperimentsMobileDependencies,
) : ComponentContext by componentContext {
    private val navigation = StackNavigation<ExperimentsMobileConfig>()
    private val coroutineScope = componentCoroutineScope()
    private val repository: ExperimentsRepository = dependencies.repositoryFactory(coroutineScope)
    private var isClosed = false

    /**
     * Стек экранов внутри mobile-фичи экспериментов.
     */
    val stack: Value<ChildStack<ExperimentsMobileConfig, ExperimentsMobileChild>> = childStack(
        source = navigation,
        serializer = ExperimentsMobileConfig.serializer(),
        initialConfiguration = ExperimentsMobileConfig.List,
        handleBackButton = true,
        childFactory = ::createChild,
    )

    /**
     * Состояние экрана экспериментов.
     */
    val state: StateFlow<ExperimentsMobileState> = repository.state

    /**
     * Переключает показ архива.
     */
    fun toggleArchivedVisibility() = repository.toggleArchivedVisibility()

    /**
     * Выбирает эксперимент по локальному id.
     */
    fun selectExperiment(id: Int) {
        repository.selectExperiment(id)
        navigation.pushToFront(ExperimentsMobileConfig.Details(id))
    }

    /**
     * Возвращает из эксперимента к списку.
     */
    fun closeExperimentDetails() {
        navigation.pop()
    }

    /**
     * Выбирает запись журнала по локальному id.
     */
    fun selectEntry(id: Int) = repository.selectEntry(id)

    /**
     * Создаёт новый эксперимент.
     */
    fun createExperiment() = repository.createExperiment()

    /**
     * Обновляет заголовок и описание выбранного эксперимента.
     */
    fun updateSelectedExperiment(title: String, description: String) =
        repository.updateSelectedExperiment(title, description)

    /**
     * Меняет архивный статус выбранного эксперимента.
     */
    fun setSelectedExperimentArchived(isArchived: Boolean) =
        repository.setSelectedExperimentArchived(isArchived)

    /**
     * Открывает или создаёт запись журнала за сегодня.
     */
    fun createTodayEntry() = repository.createTodayEntry()

    /**
     * Обновляет текст выбранной записи журнала.
     */
    fun updateSelectedEntry(content: String) = repository.updateSelectedEntry(content)

    /**
     * Создаёт напоминание для выбранного эксперимента.
     */
    fun createReminder(text: String) = repository.createReminder(text)

    /**
     * Помечает напоминание удалённым.
     */
    fun deleteReminder(id: Int) = repository.deleteReminder(id)

    /**
     * Закрывает ресурсы компонента.
     */
    fun close() {
        if (isClosed) return
        isClosed = true
        coroutineScope.cancel()
    }

    private fun componentCoroutineScope(): CoroutineScope {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        if (lifecycle.state != Lifecycle.State.DESTROYED) {
            lifecycle.doOnDestroy {
                close()
            }
        } else {
            scope.cancel()
        }

        return scope
    }

    private fun createChild(
        config: ExperimentsMobileConfig,
        componentContext: ComponentContext,
    ): ExperimentsMobileChild {
        return when (config) {
            ExperimentsMobileConfig.List -> ExperimentsMobileChild.List
            is ExperimentsMobileConfig.Details -> {
                repository.selectExperiment(config.experimentId)
                ExperimentsMobileChild.Details(config.experimentId)
            }
        }
    }
}

/**
 * Serializable config вложенной навигации experiments-фичи.
 */
@Serializable
sealed interface ExperimentsMobileConfig {
    /**
     * Список экспериментов.
     */
    @Serializable
    data object List : ExperimentsMobileConfig

    /**
     * Экран одного эксперимента.
     */
    @Serializable
    data class Details(
        val experimentId: Int,
    ) : ExperimentsMobileConfig
}

/**
 * Child-экран experiments-фичи.
 */
sealed interface ExperimentsMobileChild {
    /**
     * Child списка экспериментов.
     */
    data object List : ExperimentsMobileChild

    /**
     * Child экрана одного эксперимента.
     */
    data class Details(
        val experimentId: Int,
    ) : ExperimentsMobileChild
}
