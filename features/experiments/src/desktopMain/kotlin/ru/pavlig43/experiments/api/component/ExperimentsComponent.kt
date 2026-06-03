package ru.pavlig43.experiments.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.experiments.api.ExperimentsDependencies
import ru.pavlig43.experiments.internal.component.ExperimentEntryFilesComponent
import ru.pavlig43.experiments.internal.data.ExperimentsRepository
import ru.pavlig43.experiments.internal.di.createExperimentsModule
import ru.pavlig43.files.api.component.FilesComponent

@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentsComponent(
    componentContext: ComponentContext,
    private val dependencies: ExperimentsDependencies,
    initialSelectedExperimentId: Int? = null,
) : ComponentContext by componentContext, MainTabComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createExperimentsModule(dependencies))
    private val repository: ExperimentsRepository = scope.get()

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Эксперименты"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    private val _showArchived = MutableStateFlow(false)
    val showArchived = _showArchived.asStateFlow()

    private val _selectedExperimentId = MutableStateFlow(initialSelectedExperimentId)
    private val _selectedEntryId = MutableStateFlow<Int?>(null)

    private val _experimentDraft = MutableStateFlow(ExperimentDraft())
    private val _entryDraft = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)
    private val _filesComponent = MutableStateFlow<ExperimentEntryFilesComponent?>(null)
    private val _reminderEditorState = MutableStateFlow<ExperimentReminderEditorState?>(null)

    val message = _message.asStateFlow()
    val experimentDraft = _experimentDraft.asStateFlow()
    val entryDraft = _entryDraft.asStateFlow()
    val filesComponent: StateFlow<FilesComponent?> = _filesComponent.asStateFlow()
    val reminderEditorState = _reminderEditorState.asStateFlow()

    val experiments = _showArchived
        .flatMapLatest(repository::observeExperiments)
        .stateInComponent(emptyList())

    val selectedExperiment = _selectedExperimentId
        .flatMapLatest { id -> id?.let(repository::observeExperiment) ?: flowOf(null) }
        .stateInComponent(null)

    val entries = _selectedExperimentId
        .flatMapLatest { id -> id?.let(repository::observeEntries) ?: flowOf(emptyList()) }
        .stateInComponent(emptyList())

    val reminders = _selectedExperimentId
        .flatMapLatest { id -> id?.let(repository::observeReminders) ?: flowOf(emptyList()) }
        .stateInComponent(emptyList())

    val selectedEntry = _selectedEntryId
        .flatMapLatest { id -> id?.let(repository::observeEntry) ?: flowOf(null) }
        .stateInComponent(null)

    val uiState: StateFlow<ExperimentsUiState> = combine(
        combine(
            experiments,
            selectedExperiment,
            entries,
            reminders,
            selectedEntry,
        ) { experimentsList, experiment, entryList, reminderList, entry ->
            ExperimentsUiState(
                experiments = experimentsList.map { it.toListItem() },
                selectedExperiment = experiment?.toDetails(),
                entries = entryList.map { it.toListItem() },
                reminders = reminderList.map { it.toListItem() },
                selectedEntry = entry?.toDetails(),
            )
        },
        _showArchived,
    ) { state, showArchived ->
        state.copy(showArchived = showArchived)
    }.stateInComponent(ExperimentsUiState())

    init {
        coroutineScope.launch {
            experiments.collectLatest { list ->
                val selectedId = _selectedExperimentId.value
                if (selectedId == null) {
                    _selectedExperimentId.value = list.firstOrNull()?.id
                    return@collectLatest
                }

                val selectedLoaded = selectedExperiment.value?.id == selectedId
                if (!selectedLoaded && list.none { it.id == selectedId }) {
                    _selectedExperimentId.value = list.firstOrNull()?.id
                }
            }
        }
        coroutineScope.launch {
            selectedExperiment.collectLatest { experiment ->
                if (experiment?.isArchived == true && !_showArchived.value) {
                    _showArchived.value = true
                }
                _experimentDraft.value = ExperimentDraft(
                    title = experiment?.title.orEmpty(),
                    ideaDescription = experiment?.ideaDescription.orEmpty(),
                )
            }
        }
        coroutineScope.launch {
            entries.collectLatest { list ->
                val selectedId = _selectedEntryId.value
                val selectedStillExists = list.any { it.id == selectedId }
                if (!selectedStillExists) {
                    _selectedEntryId.value = list.firstOrNull()?.id
                }
            }
        }
        coroutineScope.launch {
            selectedEntry.collectLatest { entry ->
                _entryDraft.value = entry?.content.orEmpty()
                val currentComponent = _filesComponent.value
                if (entry == null) {
                    _filesComponent.value = null
                } else if (currentComponent == null || currentComponentEntryId != entry.id) {
                    currentComponentEntryId = entry.id
                    _filesComponent.value = ExperimentEntryFilesComponent(
                        componentContext = childContext("experiment_entry_files_${entry.id}"),
                        entryId = entry.id,
                        dependencies = dependencies.filesDependencies,
                    )
                }
            }
        }
        observeDraftSaves()
    }

    private var currentComponentEntryId: Int? = null

    fun dismissMessage() {
        _message.value = null
    }

    fun toggleArchived(showArchived: Boolean) {
        _showArchived.value = showArchived
    }

    fun selectExperiment(experimentId: Int) {
        _selectedExperimentId.value = experimentId
        _selectedEntryId.value = null
    }

    fun selectEntry(entryId: Int) {
        _selectedEntryId.value = entryId
    }

    fun onTitleChange(value: String) {
        _experimentDraft.update { it.copy(title = value) }
    }

    fun onIdeaDescriptionChange(value: String) {
        _experimentDraft.update { it.copy(ideaDescription = value) }
    }

    fun onEntryContentChange(value: String) {
        _entryDraft.value = value
    }

    fun createExperiment() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.createExperiment()
                .onSuccess { experiment ->
                    _selectedExperimentId.value = experiment.id
                    _selectedEntryId.value = null
                }
                .onFailure(::showError)
        }
    }

    fun toggleArchiveSelected() {
        val experimentId = _selectedExperimentId.value ?: return
        val shouldArchive = selectedExperiment.value?.isArchived != true
        coroutineScope.launch(Dispatchers.IO) {
            repository.setExperimentArchived(experimentId, shouldArchive)
                .onFailure(::showError)
        }
    }

    fun openTodayEntry() {
        val experimentId = _selectedExperimentId.value ?: return
        val today = getCurrentLocalDate()
        openEntryForDate(experimentId, today)
    }

    fun createTodayEntry() {
        openTodayEntry()
    }

    fun openCreateReminderDialog() {
        val experimentId = _selectedExperimentId.value ?: return
        _reminderEditorState.value = ExperimentReminderEditorState(
            experimentId = experimentId,
            reminderDateTime = getCurrentLocalDateTime(),
        )
    }

    fun openEditReminderDialog(reminderId: Int) {
        val reminder = reminders.value.firstOrNull { it.id == reminderId } ?: return
        _reminderEditorState.value = ExperimentReminderEditorState(
            reminderId = reminder.id,
            experimentId = reminder.experimentId,
            text = reminder.text,
            reminderDateTime = reminder.reminderDateTime,
        )
    }

    fun dismissReminderDialog() {
        _reminderEditorState.value = null
    }

    fun onReminderTextChange(value: String) {
        _reminderEditorState.update { state ->
            state?.copy(text = value)
        }
    }

    fun onReminderDateTimeChange(value: LocalDateTime) {
        _reminderEditorState.update { state ->
            state?.copy(reminderDateTime = value)
        }
    }

    fun saveReminder() {
        val editorState = _reminderEditorState.value ?: return
        if (editorState.text.isBlank()) {
            _message.value = "Текст напоминания не должен быть пустым"
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            val result = if (editorState.reminderId == null) {
                repository.createReminder(
                    experimentId = editorState.experimentId,
                    text = editorState.text.trim(),
                    reminderDateTime = editorState.reminderDateTime,
                ).map { }
            } else {
                val current = reminders.value.firstOrNull { it.id == editorState.reminderId }
                    ?: return@launch showError(IllegalStateException("Напоминание не найдено"))
                repository.updateReminder(
                    current.copy(
                        text = editorState.text.trim(),
                        reminderDateTime = editorState.reminderDateTime,
                        updatedAt = defaultUpdatedAt(),
                    )
                )
            }

            result
                .onSuccess { _reminderEditorState.value = null }
                .onFailure(::showError)
        }
    }

    fun deleteReminder(reminderId: Int) {
        val reminder = reminders.value.firstOrNull { it.id == reminderId } ?: return
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminder)
                .onFailure(::showError)
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeDraftSaves() {
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                _selectedExperimentId,
                _experimentDraft,
            ) { experimentId, draft -> experimentId to draft }
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { (experimentId, draft) ->
                    val current = selectedExperiment.value ?: return@collectLatest
                    if (experimentId == null || current.id != experimentId) return@collectLatest
                    if (current.title == draft.title && current.ideaDescription == draft.ideaDescription) {
                        return@collectLatest
                    }
                    repository.updateExperiment(
                        current.copy(
                            title = draft.title,
                            ideaDescription = draft.ideaDescription,
                            updatedAt = defaultUpdatedAt(),
                        )
                    ).onFailure(::showError)
                }
        }
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                _selectedEntryId,
                _entryDraft,
            ) { entryId, content -> entryId to content }
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { (entryId, content) ->
                    val current = selectedEntry.value ?: return@collectLatest
                    if (entryId == null || current.id != entryId) return@collectLatest
                    if (current.content == content) return@collectLatest
                    repository.updateEntry(
                        current.copy(
                            content = content,
                            updatedAt = defaultUpdatedAt(),
                        )
                    ).onFailure(::showError)
                }
        }
    }

    private fun openEntryForDate(
        experimentId: Int,
        date: LocalDate,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.getOrCreateEntry(experimentId, date)
                .onSuccess { entry -> _selectedEntryId.value = entry.id }
                .onFailure(::showError)
        }
    }

    private fun showError(throwable: Throwable) {
        _message.value = throwable.message ?: "Не удалось выполнить действие"
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInComponent(initial: T): StateFlow<T> {
        return this.stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            initial,
        )
    }

    private fun Experiment.toListItem(): ExperimentListItem = ExperimentListItem(
        id = id,
        title = title.ifBlank { "Без названия" },
        updatedAtText = updatedAt.format(dateTimeFormat),
        isArchived = isArchived,
    )

    private fun Experiment.toDetails(): ExperimentDetails = ExperimentDetails(
        id = id,
        title = title,
        ideaDescription = ideaDescription,
        isArchived = isArchived,
        updatedAtText = updatedAt.format(dateTimeFormat),
    )

    private fun ExperimentEntry.toListItem(): ExperimentEntryListItem = ExperimentEntryListItem(
        id = id,
        dateText = entryDate.format(dateFormat),
        preview = content.lineSequence().firstOrNull().orEmpty(),
    )

    private fun ExperimentEntry.toDetails(): ExperimentEntryDetails = ExperimentEntryDetails(
        id = id,
        dateText = entryDate.format(dateFormat),
        isToday = entryDate == getCurrentLocalDate(),
    )

    private fun ExperimentReminder.toListItem(): ExperimentReminderListItem {
        val today = getCurrentLocalDate()
        val status = when {
            reminderDateTime.date < today -> "Просрочено"
            reminderDateTime.date == today -> "Сегодня"
            else -> null
        }
        return ExperimentReminderListItem(
            id = id,
            experimentId = experimentId,
            text = text,
            reminderDateTime = reminderDateTime,
            reminderDateTimeText = reminderDateTime.format(dateTimeFormat),
            status = status,
        )
    }
}

data class ExperimentsUiState(
    val experiments: List<ExperimentListItem> = emptyList(),
    val selectedExperiment: ExperimentDetails? = null,
    val entries: List<ExperimentEntryListItem> = emptyList(),
    val reminders: List<ExperimentReminderListItem> = emptyList(),
    val selectedEntry: ExperimentEntryDetails? = null,
    val showArchived: Boolean = false,
)

data class ExperimentListItem(
    val id: Int,
    val title: String,
    val updatedAtText: String,
    val isArchived: Boolean,
)

data class ExperimentDetails(
    val id: Int,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAtText: String,
)

data class ExperimentEntryListItem(
    val id: Int,
    val dateText: String,
    val preview: String,
)

data class ExperimentReminderListItem(
    val id: Int,
    val experimentId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val reminderDateTimeText: String,
    val status: String?,
)

data class ExperimentEntryDetails(
    val id: Int,
    val dateText: String,
    val isToday: Boolean,
)

data class ExperimentDraft(
    val title: String = "",
    val ideaDescription: String = "",
)

data class ExperimentReminderEditorState(
    val reminderId: Int? = null,
    val experimentId: Int,
    val text: String = "",
    val reminderDateTime: LocalDateTime,
) {
    val isEdit: Boolean
        get() = reminderId != null
}
