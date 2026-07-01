package ru.pavlig43.nocombro.mobile.experiments.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.format
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentDetailsRepository
import ru.pavlig43.nocombro.mobile.experiments.internal.di.createMobileExperimentsModule

class ExperimentEntryComponent(
    componentContext: ComponentContext,
    private val entryId: Int,
    dependencies: ExperimentDependencies,
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createMobileExperimentsModule(dependencies))
    private val repository: ExperimentDetailsRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()

    private val editedContent = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExperimentEntryUiState> = combine(
        repository.observeEntry(entryId),
        repository.observeEntryFiles(entryId),
        editedContent,
        errorMessage,
    ) { entry, files, editedContent, errorMessage ->
        ExperimentEntryUiState(
            dateText = entry.createdAt.format(dateTimeFormat),
            content = editedContent ?: entry.content,
            files = files,
            errorMessage = errorMessage,
            isLoaded = true,
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.WhileSubscribed(5_000),
        ExperimentEntryUiState(),
    )
        .also { observeContentChanges() }

    fun onContentChange(value: String) {
        editedContent.update { value }
    }

    fun addFile(file: PlatformFile) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.addEntryFile(entryId, file)
                .onFailure(::showError)
        }
    }

    fun openFile(fileId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.openEntryFile(fileId)
                .onFailure(::showError)
        }
    }

    fun deleteFile(fileId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteEntryFile(fileId)
                .onFailure(::showError)
        }
    }

    fun dismissMessage() {
        errorMessage.update { null }
    }

    private fun showError(throwable: Throwable) {
        errorMessage.update { throwable.message ?: "Не удалось сохранить запись" }
    }

    @OptIn(FlowPreview::class)
    private fun observeContentChanges() {
        editedContent
            .filterNotNull()
            .debounce(500)
            .distinctUntilChanged()
            .onEach { content ->
                repository.updateEntryContent(entryId, content)
                    .onFailure(::showError)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }
}

data class ExperimentEntryUiState(
    val dateText: String = "",
    val content: String = "",
    val files: List<MobileExperimentEntryFile> = emptyList(),
    val errorMessage: String? = null,
    val isLoaded: Boolean = false,
)
