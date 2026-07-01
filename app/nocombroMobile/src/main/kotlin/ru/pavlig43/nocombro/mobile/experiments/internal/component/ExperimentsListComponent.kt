package ru.pavlig43.nocombro.mobile.experiments.internal.component

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
import kotlinx.coroutines.withContext
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentsListRepository
import ru.pavlig43.nocombro.mobile.internal.di.createMobileExperimentsComponentModule

@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentsListComponent(
    componentContext: ComponentContext,
    dependencies: ExperimentDependencies,
    private val onExperimentCreated: (Int) -> Unit,
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createMobileExperimentsComponentModule(dependencies))
    private val repository: ExperimentsListRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()
    private val showArchived = MutableStateFlow(false)

    val showArchivedState: StateFlow<Boolean> = showArchived.asStateFlow()

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

    fun setArchivedMode(showArchived: Boolean) {
        this.showArchived.update { showArchived }
    }

    fun createExperiment() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.createAndReturnExperiment()
                .onSuccess { experiment ->
                    withContext(Dispatchers.Main) {
                        onExperimentCreated(experiment.id)
                    }
                }
        }
    }

    fun deleteExperiment(id: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteExperiment(id)
        }
    }
}
