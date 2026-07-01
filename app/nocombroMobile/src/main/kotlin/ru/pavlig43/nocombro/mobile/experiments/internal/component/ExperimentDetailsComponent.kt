package ru.pavlig43.nocombro.mobile.experiments.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.api.component.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentDetailsRepository
import ru.pavlig43.nocombro.mobile.internal.di.createMobileExperimentsComponentModule

class ExperimentDetailsComponent(
    componentContext: ComponentContext,
    private val experimentId: Int,
    dependencies: ExperimentDependencies,
    private val onEntryOpened: (entryId: Int) -> Unit = {},
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createMobileExperimentsComponentModule(dependencies))
    private val repository: ExperimentDetailsRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()

    private val experimentDraft = MutableStateFlow(ExperimentDraft())
    val experimentDraftState: StateFlow<ExperimentDraft> = experimentDraft.asStateFlow()
    private val message = MutableStateFlow<String?>(null)
    private val reminderEditor = MutableStateFlow<ExperimentReminderEditorState?>(null)


    private val experiment = repository.observeExperiment(experimentId)
        .stateInComponent(null)

    private val entries = repository.observeEntries(experimentId)
        .stateInComponent(emptyList())

    private val reminders = repository.observeReminders(experimentId)
        .stateInComponent(emptyList())

    val uiState: StateFlow<ExperimentDetailsUiState> = combine(
        combine(
            experiment,
            entries,
            reminders,
            experimentDraft,
        ) { experiment, entries, reminders, draft ->
            ExperimentDetailsUiState(
                experiment = experiment?.toDetails(),
                entries = entries.map { it.toListItem() },
                reminders = reminders.map { it.toListItem() },
                experimentDraft = draft,
            )
        },
        combine(
            message,
            reminderEditor,
        ) { message, reminderEditor ->
            DetailsExtraState(message, reminderEditor)
        },
    ) { state, extra ->
        state.copy(
            message = extra.message,
            reminderEditor = extra.reminderEditor,
        )
    }.stateInComponent(ExperimentDetailsUiState())

    init {
        coroutineScope.launch {
            experiment.collectLatest { current ->
                experimentDraft.update {
                    ExperimentDraft(
                        title = current?.title.orEmpty(),
                        ideaDescription = current?.ideaDescription.orEmpty(),
                    )
                }
            }
        }
        observeDraftSaves()
    }

    fun onTitleChange(value: String) {
        experimentDraft.update { it.copy(title = value) }
    }

    fun onIdeaChange(value: String) {
        experimentDraft.update { it.copy(ideaDescription = value) }
    }

    fun openEntryScreen(entryId: Int) {
        onEntryOpened(entryId)
    }

    fun openTodayEntry() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.createEntryForDate(experimentId, getCurrentLocalDate())
                .onSuccess { entry ->
                    onEntryOpened(entry.id)
                }
                .onFailure(::showError)
        }
    }

    fun toggleArchive() {
        val shouldArchive = experiment.value?.isArchived != true
        coroutineScope.launch(Dispatchers.IO) {
            repository.setExperimentArchived(experimentId, shouldArchive)
                .onFailure(::showError)
        }
    }

    fun openCreateReminder() {
        reminderEditor.update {
            ExperimentReminderEditorState(
                experimentId = experimentId,
                reminderDateTime = getCurrentLocalDateTime(),
            )
        }
    }

    fun openEditReminder(reminderId: Int) {
        val reminder = reminders.value.firstOrNull { it.id == reminderId } ?: return
        reminderEditor.update {
            ExperimentReminderEditorState(
                reminderId = reminder.id,
                experimentId = reminder.experimentId,
                text = reminder.text,
                reminderDateTime = reminder.reminderDateTime,
            )
        }
    }

    fun dismissReminderEditor() {
        reminderEditor.update { null }
    }

    fun onReminderTextChange(value: String) {
        reminderEditor.update { state -> state?.copy(text = value) }
    }

    fun onReminderDateTimeChange(value: LocalDateTime) {
        reminderEditor.update { state -> state?.copy(reminderDateTime = value) }
    }

    fun saveReminder() {
        val state = reminderEditor.value ?: return
        val text = state.text.trim()
        if (text.isBlank()) {
            message.update { "Введите текст напоминания" }
            return
        }
        val dateTime = state.reminderDateTime

        coroutineScope.launch(Dispatchers.IO) {
            val result = if (state.reminderId == null) {
                repository.createReminder(experimentId, text, dateTime).map { }
            } else {
                repository.updateReminder(state.reminderId, text, dateTime)
            }
            result
                .onSuccess { reminderEditor.update { null } }
                .onFailure(::showError)
        }
    }

    fun deleteReminder(reminderId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminderId)
                .onFailure(::showError)
        }
    }

    fun dismissMessage() {
        message.update { null }
    }

    @OptIn(FlowPreview::class)
    private fun observeDraftSaves() {
        coroutineScope.launch(Dispatchers.IO) {
            experimentDraft
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { draft ->
                    val current = experiment.value ?: return@collectLatest
                    if (current.title == draft.title && current.ideaDescription == draft.ideaDescription) {
                        return@collectLatest
                    }
                    repository.updateExperimentDraft(
                        experimentId = experimentId,
                        title = draft.title,
                        ideaDescription = draft.ideaDescription,
                    ).onFailure(::showError)
                }
        }
    }

    private fun showError(throwable: Throwable) {
        message.update { throwable.message ?: "Не удалось выполнить действие" }
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInComponent(initial: T): StateFlow<T> {
        return stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initial,
        )
    }

    private fun MobileExperiment.toDetails(): ExperimentDetails = ExperimentDetails(
        id = id,
        title = title,
        ideaDescription = ideaDescription,
        isArchived = isArchived,
        updatedAtText = updatedAt.format(dateTimeFormat),
    )

    private fun MobileExperimentEntry.toListItem(): ExperimentEntryListItem = ExperimentEntryListItem(
        id = id,
        dateText = createdAt.format(dateTimeFormat),
        preview = content.lineSequence().firstOrNull().orEmpty(),
    )

    private fun MobileExperimentReminder.toListItem(): ExperimentReminderListItem {
        val today = getCurrentLocalDate()
        val status = when {
            reminderDateTime.date < today -> "Просрочено"
            reminderDateTime.date == today -> "Сегодня"
            else -> null
        }
        return ExperimentReminderListItem(
            id = id,
            text = text,
            reminderDateTimeText = reminderDateTime.format(dateTimeFormat),
            status = status,
        )
    }
}

data class ExperimentDetailsUiState(
    val experiment: ExperimentDetails? = null,
    val entries: List<ExperimentEntryListItem> = emptyList(),
    val reminders: List<ExperimentReminderListItem> = emptyList(),
    val experimentDraft: ExperimentDraft = ExperimentDraft(),
    val reminderEditor: ExperimentReminderEditorState? = null,
    val message: String? = null,
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
    val text: String,
    val reminderDateTimeText: String,
    val status: String?,
)

data class ExperimentDraft(
    val title: String = "",
    val ideaDescription: String = "",
)

data class ExperimentReminderEditorState(
    val reminderId: Int? = null,
    val experimentId: Int,
    val text: String = "",
    val reminderDateTime: LocalDateTime = getCurrentLocalDateTime(),
) {
    val isEdit: Boolean
        get() = reminderId != null
}

private data class DetailsExtraState(
    val message: String?,
    val reminderEditor: ExperimentReminderEditorState?,
)
