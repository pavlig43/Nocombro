package ru.pavlig43.nocombro.mobile.experiments

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext

private fun createExperimentsMobileModule(
    dependencies: ExperimentDependencies,
): List<Module> = listOf(
    module {
        single {
            ExperimentsListRepository(
                db = dependencies.database,
            )
        }
    }
)

/**
 * Decompose-компонент списка mobile-экспериментов.
 *
 * Создаёт Koin scope из зависимостей, сам держит выбранный режим и сразу отдаёт
 * [StateFlow] для UI.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentsListComponent(
    componentContext: ComponentContext,
    dependencies: ExperimentDependencies,
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createExperimentsMobileModule(dependencies))
    private val repository: ExperimentsListRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()
    private val showArchived = MutableStateFlow(false)

    /**
     * Текущий режим списка.
     */
    val showArchivedState: StateFlow<Boolean> = showArchived.asStateFlow()

    /**
     * Эксперименты для текущего режима списка.
     */
    val experiments: StateFlow<List<MobileExperiment>> = showArchived
        .flatMapLatest { archived ->
            if (archived) {
                repository.observeArchivedExperiments()
            } else {
                repository.observeActiveExperiments()
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /**
     * Задаёт режим списка.
     */
    fun setArchivedMode(showArchived: Boolean) {
        this.showArchived.update { showArchived }
    }

    /**
     * Создаёт эксперимент в локальной БД.
     */
    fun createExperiment() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.createExperiment()
        }
    }

    /**
     * Помечает эксперимент удалённым.
     */
    fun deleteExperiment(id: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteExperiment(id)
        }
    }
}
