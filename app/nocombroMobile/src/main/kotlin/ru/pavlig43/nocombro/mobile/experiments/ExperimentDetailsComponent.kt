package ru.pavlig43.nocombro.mobile.experiments

import com.arkivanov.decompose.ComponentContext
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
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.datetime.getCurrentLocalDateTime

private fun createExperimentDetailsModule(
    dependencies: ExperimentDependencies,
): List<Module> = listOf(
    module {
        single {
            ExperimentDetailsRepository(
                db = dependencies.database,
            )
        }
    }
)

/**
 * Decompose-компонент деталей mobile-эксперимента.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExperimentDetailsComponent(
    componentContext: ComponentContext,
    private val experimentId: Int,
    dependencies: ExperimentDependencies,
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createExperimentDetailsModule(dependencies))
    private val repository: ExperimentDetailsRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()

    private val selectedEntryId = MutableStateFlow<Int?>(null)
    private val experimentDraft = MutableStateFlow(ExperimentDraft())
    private val entryDraft = MutableStateFlow("")
    private val message = MutableStateFlow<String?>(null)
    private val reminderEditor = MutableStateFlow<ExperimentReminderEditorState?>(null)

    /**
     * Draft полей эксперимента.
     */
    val experimentDraftState: StateFlow<ExperimentDraft> = experimentDraft.asStateFlow()

    /**
     * Draft текста записи.
     */
    val entryDraftState: StateFlow<String> = entryDraft.asStateFlow()

    private val experiment = repository.observeExperiment(experimentId)
        .stateInComponent(null)

    private val entries = repository.observeEntries(experimentId)
        .stateInComponent(emptyList())

    private val reminders = repository.observeReminders(experimentId)
        .stateInComponent(emptyList())

    private val selectedEntry = selectedEntryId
        .flatMapLatest { id -> id?.let(repository::observeEntry) ?: flowOf(null) }
        .stateInComponent(null)

    /**
     * Состояние экрана деталей.
     */
    val uiState: StateFlow<ExperimentDetailsUiState> = combine(
        combine(
            experiment,
            entries,
            reminders,
            selectedEntry,
            experimentDraft,
        ) { experiment, entries, reminders, selectedEntry, draft ->
            ExperimentDetailsUiState(
                experiment = experiment?.toDetails(),
                entries = entries.map { it.toListItem() },
                reminders = reminders.map { it.toListItem() },
                selectedEntry = selectedEntry?.toDetails(),
                experimentDraft = draft,
            )
        },
        combine(
            entryDraft,
            message,
            reminderEditor,
        ) { entryDraft, message, reminderEditor ->
            Triple(entryDraft, message, reminderEditor)
        },
    ) { state, extra ->
        state.copy(
            entryDraft = extra.first,
            message = extra.second,
            reminderEditor = extra.third,
        )
    }.stateInComponent(ExperimentDetailsUiState())

    init {
        coroutineScope.launch {
            experiment.collectLatest { current ->
                experimentDraft.value = ExperimentDraft(
                    title = current?.title.orEmpty(),
                    ideaDescription = current?.ideaDescription.orEmpty(),
                )
            }
        }
        coroutineScope.launch {
            entries.collectLatest { list ->
                val selectedId = selectedEntryId.value
                if (list.none { it.id == selectedId }) {
                    selectedEntryId.value = list.firstOrNull()?.id
                }
            }
        }
        coroutineScope.launch {
            selectedEntry.collectLatest { entry ->
                entryDraft.value = entry?.content.orEmpty()
            }
        }
        observeDraftSaves()
    }

    /**
     * Меняет draft названия.
     */
    fun onTitleChange(value: String) {
        experimentDraft.update { it.copy(title = value) }
    }

    /**
     * Меняет draft идеи.
     */
    fun onIdeaChange(value: String) {
        experimentDraft.update { it.copy(ideaDescription = value) }
    }

    /**
     * Выбирает запись журнала.
     */
    fun selectEntry(entryId: Int) {
        selectedEntryId.value = entryId
    }

    /**
     * Меняет draft текста записи.
     */
    fun onEntryContentChange(value: String) {
        entryDraft.value = value
    }

    /**
     * Создаёт или открывает запись за сегодня.
     */
    fun openTodayEntry() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.getOrCreateEntry(experimentId, getCurrentLocalDate())
                .onSuccess { entry -> selectedEntryId.value = entry.id }
                .onFailure(::showError)
        }
    }

    /**
     * Архивирует или возвращает эксперимент.
     */
    fun toggleArchive() {
        val shouldArchive = experiment.value?.isArchived != true
        coroutineScope.launch(Dispatchers.IO) {
            repository.setExperimentArchived(experimentId, shouldArchive)
                .onFailure(::showError)
        }
    }

    /**
     * Открывает форму нового напоминания.
     */
    fun openCreateReminder() {
        reminderEditor.value = ExperimentReminderEditorState(
            experimentId = experimentId,
            reminderDateTime = getCurrentLocalDateTime(),
        )
    }

    /**
     * Открывает форму правки напоминания.
     */
    fun openEditReminder(reminderId: Int) {
        val reminder = reminders.value.firstOrNull { it.id == reminderId } ?: return
        reminderEditor.value = ExperimentReminderEditorState(
            reminderId = reminder.id,
            experimentId = reminder.experimentId,
            text = reminder.text,
            reminderDateTime = reminder.reminderDateTime,
        )
    }

    /**
     * Закрывает форму напоминания.
     */
    fun dismissReminderEditor() {
        reminderEditor.value = null
    }

    /**
     * Меняет текст напоминания.
     */
    fun onReminderTextChange(value: String) {
        reminderEditor.update { state -> state?.copy(text = value) }
    }

    /**
     * Меняет дату и время напоминания.
     */
    fun onReminderDateTimeChange(value: LocalDateTime) {
        reminderEditor.update { state -> state?.copy(reminderDateTime = value) }
    }

    /**
     * Сохраняет напоминание.
     */
    fun saveReminder() {
        val state = reminderEditor.value ?: return
        val text = state.text.trim()
        if (text.isBlank()) {
            message.value = "Введите текст напоминания"
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
                .onSuccess { reminderEditor.value = null }
                .onFailure(::showError)
        }
    }

    /**
     * Удаляет напоминание.
     */
    fun deleteReminder(reminderId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminderId)
                .onFailure(::showError)
        }
    }

    /**
     * Скрывает сообщение.
     */
    fun dismissMessage() {
        message.value = null
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
        coroutineScope.launch(Dispatchers.IO) {
            combine(
                selectedEntryId,
                entryDraft,
            ) { entryId, draft -> entryId to draft }
                .debounce(500)
                .distinctUntilChanged()
                .collectLatest { (entryId, draft) ->
                    val current = selectedEntry.value ?: return@collectLatest
                    if (entryId == null || current.id != entryId || current.content == draft) {
                        return@collectLatest
                    }
                    repository.updateEntryContent(entryId, draft)
                        .onFailure(::showError)
                }
        }
    }

    private fun showError(throwable: Throwable) {
        message.value = throwable.message ?: "Не удалось выполнить действие"
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
        dateText = entryDate.format(dateFormat),
        preview = content.lineSequence().firstOrNull().orEmpty(),
    )

    private fun MobileExperimentEntry.toDetails(): ExperimentEntryDetails = ExperimentEntryDetails(
        id = id,
        dateText = entryDate.format(dateFormat),
        isToday = entryDate == getCurrentLocalDate(),
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
    val selectedEntry: ExperimentEntryDetails? = null,
    val experimentDraft: ExperimentDraft = ExperimentDraft(),
    val entryDraft: String = "",
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

data class ExperimentEntryDetails(
    val id: Int,
    val dateText: String,
    val isToday: Boolean,
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
