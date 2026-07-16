package ru.pavlig43.nocombro.mobile.experiments.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
import ru.pavlig43.nocombro.mobile.experiments.api.ExperimentDependencies
import ru.pavlig43.nocombro.mobile.experiments.internal.data.ExperimentDetailsRepository
import ru.pavlig43.nocombro.mobile.experiments.internal.di.createMobileExperimentsModule

/**
 * Экран одного эксперимента: держит форму, список записей, напоминания и автосохранение.
 *
 * Компонент не хранит бизнес-данные сам. Источник правды — Room через [ExperimentDetailsRepository],
 * а локальные state нужны только для редактируемого текста, диалогов и сообщений.
 */
@Suppress("TooManyFunctions")
class ExperimentDetailsComponent(
    componentContext: ComponentContext,
    private val experimentId: Int,
    dependencies: ExperimentDependencies,
    private val onEntryOpened: (entryId: Int) -> Unit = {},
) : ComponentContext by componentContext {
    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(createMobileExperimentsModule(dependencies))
    private val repository: ExperimentDetailsRepository = scope.get()
    private val coroutineScope = componentCoroutineScope()

    /** Черновик полей формы, который автосохраняется после паузы ввода. */
    private val _experimentDraft = MutableStateFlow(ExperimentDraft())

    /** Текст ошибки или валидации, показанный над формой. */
    private val _message = MutableStateFlow<String?>(null)

    /** Открытый редактор напоминания; null значит, что диалог закрыт. */
    private val _reminderEditor = MutableStateFlow<ExperimentReminderEditorState?>(null)

    private val selectedExperiment: StateFlow<MobileExperiment?> = repository.observeExperiment(experimentId)
        .onEach { experiment ->
            _experimentDraft.value = experiment?.toDraft() ?: ExperimentDraft()
        }
        .stateInComponent(null)

    private val entries = repository.observeEntries(experimentId)
        .stateInComponent(emptyList())

    private val reminders = repository.observeReminders(experimentId)
        .stateInComponent(emptyList())

    private val contentState: StateFlow<ExperimentDetailsContentState> = combine(
        selectedExperiment,
        entries,
        reminders,
        _experimentDraft,
    ) { experiment, entries, reminders, draft ->
        ExperimentDetailsContentState(
            experiment = experiment?.toDetails(),
            entries = entries.map { it.toListItem() },
            reminders = reminders.map { it.toListItem() },
            experimentDraft = draft,
        )
    }.stateInComponent(ExperimentDetailsContentState())

    val uiState: StateFlow<ExperimentDetailsUiState> = combine(
        contentState,
        _message,
        _reminderEditor,
    ) { content, message, reminderEditor ->
        ExperimentDetailsUiState(
            experiment = content.experiment,
            entries = content.entries,
            reminders = content.reminders,
            experimentDraft = content.experimentDraft,
            reminderEditor = reminderEditor,
            message = message,
        )
    }.stateInComponent(ExperimentDetailsUiState())
        .also { observeDraftSaves() }

    /** Обновляет локальный черновик названия; запись в БД пойдёт через автосохранение. */
    fun onTitleChange(value: String) {
        _experimentDraft.update { it.copy(title = value) }
    }

    /** Обновляет локальный черновик идеи; запись в БД пойдёт через автосохранение. */
    fun onIdeaChange(value: String) {
        _experimentDraft.update { it.copy(ideaDescription = value) }
    }

    /** Передаёт выбранную запись родительской навигации experiments. */
    fun openEntryScreen(entryId: Int) {
        onEntryOpened(entryId)
    }

    /**
     * Создаёт запись журнала для текущей локальной даты устройства.
     *
     * Дата нужна для группировки записей в БД и берётся в момент нажатия кнопки.
     * После создания открывает экран новой записи.
     */
    fun createEntryForCurrentDate() {
        coroutineScope.launch(Dispatchers.IO) {
            repository.createEntryForDate(experimentId, getCurrentLocalDate())
                .onSuccess { entry ->
                    onEntryOpened(entry.id)
                }
                .onFailure(::showError)
        }
    }

    /** Ставит архивный флаг в заданное состояние без локального вычисления обратного значения. */
    fun setArchived(isArchived: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.setExperimentArchived(experimentId, isArchived)
                .onFailure(::showError)
        }
    }

    /** Открывает пустой редактор напоминания для текущего эксперимента. */
    fun showNewReminderEditor() {
        _reminderEditor.value = ExperimentReminderEditorState(
            experimentId = experimentId,
            reminderDateTime = getCurrentLocalDateTime(),
        )
    }

    /** Загружает выбранное напоминание из уже подписанного списка и открывает его редактор. */
    fun showReminderEditor(reminderId: Int) {
        val reminder = reminders.value.firstOrNull { it.id == reminderId } ?: return
        _reminderEditor.value = ExperimentReminderEditorState(
            reminderId = reminder.id,
            experimentId = reminder.experimentId,
            text = reminder.text,
            reminderDateTime = reminder.reminderDateTime,
        )
    }

    /** Закрывает диалог напоминания без сохранения текущего черновика диалога. */
    fun closeReminderEditor() {
        _reminderEditor.value = null
    }

    /** Правит текст в открытом редакторе напоминания; без открытого диалога вызов ничего не делает. */
    fun onReminderTextChange(value: String) {
        _reminderEditor.update { editor -> editor?.copy(text = value) }
    }

    /** Правит дату и время в открытом редакторе напоминания. */
    fun onReminderDateTimeChange(value: LocalDateTime) {
        _reminderEditor.update { editor -> editor?.copy(reminderDateTime = value) }
    }

    /** Валидирует открытый редактор напоминания и сохраняет новую или изменённую строку. */
    fun saveReminder() {
        val editor = _reminderEditor.value ?: return
        val text = editor.text.trim()
        if (text.isBlank()) {
            _message.value = "Введите текст напоминания"
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            val result: Result<Unit> = if (editor.isEdit) {
                repository.updateReminder(editor.reminderId ?: return@launch, text, editor.reminderDateTime)
            } else {
                repository.createReminder(experimentId, text, editor.reminderDateTime)
                    .fold(
                        onSuccess = { Result.success(Unit) },
                        onFailure = { Result.failure(it) },
                    )
            }

            result
                .onSuccess { _reminderEditor.value = null }
                .onFailure(::showError)
        }
    }

    /** Помечает напоминание удалённым; физическое удаление не делает, чтобы не ломать sync-модель. */
    fun deleteReminder(reminderId: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            repository.deleteReminder(reminderId)
                .onFailure(::showError)
        }
    }

    /** Скрывает текущее сообщение об ошибке или валидации. */
    fun dismissMessage() {
        _message.value = null
    }

    /** Сохраняет черновик полей после паузы ввода, чтобы не писать в БД на каждый символ. */
    @OptIn(FlowPreview::class)
    private fun observeDraftSaves() {
        _experimentDraft
            .debounce(500)
            .distinctUntilChanged()
            .onEach { draft ->
                val experiment = selectedExperiment.value ?: return@onEach
                if (experiment.title == draft.title && experiment.ideaDescription == draft.ideaDescription) {
                    return@onEach
                }
                repository.updateExperimentDraft(
                    experimentId = experimentId,
                    title = draft.title,
                    ideaDescription = draft.ideaDescription,
                ).onFailure(::showError)
            }
            .flowOn(Dispatchers.IO)
            .launchIn(coroutineScope)
    }

    private fun showError(throwable: Throwable) {
        _message.value = throwable.message ?: "Не удалось выполнить действие"
    }

    private fun <T> kotlinx.coroutines.flow.Flow<T>.stateInComponent(initial: T): StateFlow<T> {
        return stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = initial,
        )
    }

    private fun MobileExperiment.toDraft(): ExperimentDraft = ExperimentDraft(
        title = title,
        ideaDescription = ideaDescription,
    )

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
            experimentId = experimentId,
            text = text,
            reminderDateTime = reminderDateTime,
            reminderDateTimeText = reminderDateTime.format(dateTimeFormat),
            status = status,
        )
    }
}

/** Данные формы без диалога напоминаний и сообщений. */
private data class ExperimentDetailsContentState(
    val experiment: ExperimentDetails? = null,
    val entries: List<ExperimentEntryListItem> = emptyList(),
    val reminders: List<ExperimentReminderListItem> = emptyList(),
    val experimentDraft: ExperimentDraft = ExperimentDraft(),
)

/** Полное состояние экрана; experiment бывает null, пока строка не пришла из БД или уже удалена. */
data class ExperimentDetailsUiState(
    val experiment: ExperimentDetails? = null,
    val entries: List<ExperimentEntryListItem> = emptyList(),
    val reminders: List<ExperimentReminderListItem> = emptyList(),
    val experimentDraft: ExperimentDraft = ExperimentDraft(),
    val reminderEditor: ExperimentReminderEditorState? = null,
    val message: String? = null,
)

/** Заголовочные данные эксперимента, уже подготовленные для экрана. */
data class ExperimentDetails(
    val id: Int,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    val updatedAtText: String,
)

/** Строка списка записей с датой и коротким предпросмотром текста. */
data class ExperimentEntryListItem(
    val id: Int,
    val dateText: String,
    val preview: String,
)

/** Строка списка напоминаний с готовым текстом даты и статусом относительно текущей даты. */
data class ExperimentReminderListItem(
    val id: Int,
    val experimentId: Int,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val reminderDateTimeText: String,
    val status: String?,
)

/** Редактируемые поля эксперимента, которые можно безопасно автосохранять. */
data class ExperimentDraft(
    val title: String = "",
    val ideaDescription: String = "",
)

/** Состояние диалога напоминания; null у владельца state значит, что диалог закрыт. */
data class ExperimentReminderEditorState(
    val reminderId: Int? = null,
    val experimentId: Int,
    val text: String = "",
    val reminderDateTime: LocalDateTime = getCurrentLocalDateTime(),
) {
    val isEdit: Boolean
        get() = reminderId != null
}
