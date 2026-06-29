package ru.pavlig43.nocombro.mobile.experiments

import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentReminderEntity
import ru.pavlig43.nocombro.mobile.experiments.data.MobileExperimentsDatabase
import ru.pavlig43.nocombro.mobile.experiments.data.toModel

/**
 * Контракт хранилища и команд для mobile-экрана экспериментов.
 */
interface ExperimentsRepository {
    /**
     * Единый state экрана.
     */
    val state: StateFlow<ExperimentsMobileState>

    /**
     * Переключает показ архивных экспериментов.
     */
    fun toggleArchivedVisibility()

    /**
     * Выбирает эксперимент по локальному id.
     */
    fun selectExperiment(id: Int)

    /**
     * Выбирает запись журнала по локальному id.
     */
    fun selectEntry(id: Int)

    /**
     * Создаёт новый эксперимент.
     */
    fun createExperiment()

    /**
     * Обновляет выбранный эксперимент.
     */
    fun updateSelectedExperiment(title: String, description: String)

    /**
     * Меняет архивный статус выбранного эксперимента.
     */
    fun setSelectedExperimentArchived(isArchived: Boolean)

    /**
     * Открывает или создаёт запись за сегодня.
     */
    fun createTodayEntry()

    /**
     * Обновляет выбранную запись журнала.
     */
    fun updateSelectedEntry(content: String)

    /**
     * Создаёт напоминание для выбранного эксперимента.
     */
    fun createReminder(text: String)

    /**
     * Помечает напоминание удалённым.
     */
    fun deleteReminder(id: Int)

    /**
     * Запускает синхронизацию snapshot-а.
     */
    suspend fun sync()
}

/**
 * Транспорт синхронизации экспериментов.
 */
interface ExperimentSyncTransport {
    /**
     * Отправляет локальный snapshot во внешний backend.
     */
    suspend fun sync(snapshot: ExperimentSyncSnapshot): Result<Unit>
}

/**
 * Room-реализация [ExperimentsRepository] для mobile-приложения.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomExperimentsRepository(
    private val db: MobileExperimentsDatabase,
    private val coroutineScope: CoroutineScope,
    private val syncTransport: ExperimentSyncTransport,
) : ExperimentsRepository {
    private val experimentDao = db.experimentDao
    private val experimentEntryDao = db.experimentEntryDao
    private val experimentReminderDao = db.experimentReminderDao

    private val showArchived = MutableStateFlow(false)
    private val selectedExperimentId = MutableStateFlow<Int?>(null)
    private val selectedEntryId = MutableStateFlow<Int?>(null)
    private val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    private val experiments: Flow<List<MobileExperiment>> = showArchived
        .flatMapLatest { experimentDao.observeExperiments(it) }
        .map { list -> list.map { it.toModel() } }

    private val selectedExperiment: Flow<MobileExperiment?> = selectedExperimentId
        .flatMapLatest { id -> id?.let { experimentDao.observeExperiment(it) } ?: flowOf(null) }
        .map { it?.toModel() }

    private val entries: Flow<List<MobileExperimentEntry>> = selectedExperimentId
        .flatMapLatest { id -> id?.let { experimentEntryDao.observeEntries(it) } ?: flowOf(emptyList()) }
        .map { list -> list.map { it.toModel() } }

    private val selectedEntry: Flow<MobileExperimentEntry?> = selectedEntryId
        .flatMapLatest { id -> id?.let { experimentEntryDao.observeEntry(it) } ?: flowOf(null) }
        .map { it?.toModel() }

    private val reminders: Flow<List<MobileExperimentReminder>> = selectedExperimentId
        .flatMapLatest { id -> id?.let { experimentReminderDao.observeReminders(it) } ?: flowOf(emptyList()) }
        .map { list -> list.map { it.toModel() } }

    override val state: StateFlow<ExperimentsMobileState> = combine(
        combine(
            experiments,
            selectedExperiment,
            entries,
            selectedEntry,
            reminders,
        ) { experimentList, experiment, entryList, entry, reminderList ->
            ExperimentsMobileState(
                experiments = experimentList,
                selectedExperiment = experiment,
                entries = entryList,
                selectedEntry = entry,
                reminders = reminderList,
            )
        },
        showArchived,
        syncStatus,
    ) { state, archived, status ->
        state.copy(
            showArchived = archived,
            syncStatus = status,
        )
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExperimentsMobileState(),
    )

    init {
        coroutineScope.launch {
            seedIfEmpty()
        }
        coroutineScope.launch {
            experiments.collect { list ->
                val selectedId = selectedExperimentId.value
                if (selectedId == null || list.none { it.id == selectedId }) {
                    selectedExperimentId.value = list.firstOrNull()?.id
                }
            }
        }
        coroutineScope.launch {
            entries.collect { list ->
                val selectedId = selectedEntryId.value
                if (selectedId == null || list.none { it.id == selectedId }) {
                    selectedEntryId.value = list.firstOrNull()?.id
                }
            }
        }
    }

    override fun toggleArchivedVisibility() {
        showArchived.update { !it }
    }

    override fun selectExperiment(id: Int) {
        selectedExperimentId.value = id
        selectedEntryId.value = null
    }

    override fun selectEntry(id: Int) {
        selectedEntryId.value = id
    }

    override fun createExperiment() {
        coroutineScope.launch {
            val experiment = MobileExperimentEntity(
                title = "Новый эксперимент",
                syncId = newSyncId(),
                updatedAt = getCurrentLocalDateTime(),
            )
            selectedExperimentId.value = experimentDao.create(experiment).toInt()
            selectedEntryId.value = null
        }
    }

    override fun updateSelectedExperiment(title: String, description: String) {
        val selected = state.value.selectedExperiment ?: return
        coroutineScope.launch {
            experimentDao.upsert(
                MobileExperimentEntity(
                    id = selected.id,
                    syncId = selected.syncId,
                    title = title,
                    ideaDescription = description,
                    isArchived = selected.isArchived,
                    updatedAt = getCurrentLocalDateTime(),
                    deletedAt = selected.deletedAt,
                )
            )
        }
    }

    override fun setSelectedExperimentArchived(isArchived: Boolean) {
        val selected = state.value.selectedExperiment ?: return
        coroutineScope.launch {
            experimentDao.upsert(
                MobileExperimentEntity(
                    id = selected.id,
                    syncId = selected.syncId,
                    title = selected.title,
                    ideaDescription = selected.ideaDescription,
                    isArchived = isArchived,
                    updatedAt = getCurrentLocalDateTime(),
                    deletedAt = selected.deletedAt,
                )
            )
        }
    }

    override fun createTodayEntry() {
        val experiment = state.value.selectedExperiment ?: return
        coroutineScope.launch {
            val existing = experimentEntryDao.getEntryByExperimentAndDate(
                experimentId = experiment.id,
                entryDate = getCurrentLocalDate(),
            )
            if (existing != null) {
                selectedEntryId.value = existing.id
                return@launch
            }

            val entry = MobileExperimentEntryEntity(
                experimentId = experiment.id,
                entryDate = getCurrentLocalDate(),
                syncId = newSyncId(),
                updatedAt = getCurrentLocalDateTime(),
            )
            selectedEntryId.value = experimentEntryDao.create(entry).toInt()
            touchExperiment(experiment.id)
        }
    }

    override fun updateSelectedEntry(content: String) {
        val selected = state.value.selectedEntry ?: return
        coroutineScope.launch {
            experimentEntryDao.upsert(
                MobileExperimentEntryEntity(
                    id = selected.id,
                    syncId = selected.syncId,
                    experimentId = selected.experimentId,
                    entryDate = selected.entryDate,
                    content = content,
                    updatedAt = getCurrentLocalDateTime(),
                    deletedAt = selected.deletedAt,
                )
            )
            touchExperiment(selected.experimentId)
        }
    }

    override fun createReminder(text: String) {
        val experiment = state.value.selectedExperiment ?: return
        if (text.isBlank()) return

        coroutineScope.launch {
            experimentReminderDao.create(
                MobileExperimentReminderEntity(
                    experimentId = experiment.id,
                    text = text.trim(),
                    reminderDateTime = getCurrentLocalDateTime(),
                    syncId = newSyncId(),
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
            touchExperiment(experiment.id)
        }
    }

    override fun deleteReminder(id: Int) {
        coroutineScope.launch {
            val reminder = experimentReminderDao.getReminder(id) ?: return@launch
            val deletedAt = getCurrentLocalDateTime()
            experimentReminderDao.upsert(
                reminder.copy(
                    updatedAt = deletedAt,
                    deletedAt = deletedAt,
                )
            )
            touchExperiment(reminder.experimentId)
        }
    }

    override suspend fun sync() {
        syncStatus.value = SyncStatus.Running
        val result = syncTransport.sync(buildSyncSnapshot())
        syncStatus.value = result.fold(
            onSuccess = { SyncStatus.Synced(getCurrentLocalDateTime()) },
            onFailure = { SyncStatus.Failed(it.message ?: "Не удалось синхронизировать") },
        )
    }

    private suspend fun seedIfEmpty() {
        if (experimentDao.getAll().isNotEmpty()) {
            localizeLegacySeedData()
            return
        }

        val experiment = MobileExperimentEntity(
            title = "Новый эксперимент",
            ideaDescription = "Проверьте гипотезу и ведите журнал изменений.",
            syncId = newSyncId(),
            updatedAt = getCurrentLocalDateTime(),
        )
        val experimentId = experimentDao.create(experiment).toInt()
        experimentEntryDao.create(
            MobileExperimentEntryEntity(
                experimentId = experimentId,
                entryDate = getCurrentLocalDate(),
                content = "Первая заметка по эксперименту.",
                syncId = newSyncId(),
                updatedAt = getCurrentLocalDateTime(),
            )
        )
        selectedExperimentId.value = experimentId
    }

    private suspend fun localizeLegacySeedData() {
        experimentDao.getAll()
            .filter {
                it.title == LEGACY_SEED_EXPERIMENT_TITLE ||
                    it.ideaDescription == LEGACY_SEED_EXPERIMENT_DESCRIPTION
            }
            .forEach { experiment ->
                experimentDao.upsert(
                    experiment.copy(
                        title = if (experiment.title == LEGACY_SEED_EXPERIMENT_TITLE) {
                            "Новый эксперимент"
                        } else {
                            experiment.title
                        },
                        ideaDescription = if (
                            experiment.ideaDescription == LEGACY_SEED_EXPERIMENT_DESCRIPTION
                        ) {
                            "Проверьте гипотезу и ведите журнал изменений."
                        } else {
                            experiment.ideaDescription
                        },
                    )
                )
            }

        experimentEntryDao.getAll()
            .filter { it.content == LEGACY_SEED_ENTRY_CONTENT }
            .forEach { entry ->
                experimentEntryDao.upsert(
                    entry.copy(content = "Первая заметка по эксперименту.")
                )
            }
    }

    private suspend fun touchExperiment(id: Int) {
        val experiment = experimentDao.getExperiment(id) ?: return
        experimentDao.upsert(experiment.copy(updatedAt = getCurrentLocalDateTime()))
    }

    private suspend fun buildSyncSnapshot(): ExperimentSyncSnapshot {
        val experiments = experimentDao.getAll()
        val experimentSyncIds = experiments.associate { it.id to it.syncId }
        val entries = experimentEntryDao.getAll().mapNotNull { entry ->
            val experimentSyncId = experimentSyncIds[entry.experimentId] ?: return@mapNotNull null
            ExperimentEntrySyncRow(
                syncId = entry.syncId,
                experimentSyncId = experimentSyncId,
                entryDate = entry.entryDate,
                content = entry.content,
                updatedAt = entry.updatedAt,
                deletedAt = entry.deletedAt,
            )
        }
        val reminders = experimentReminderDao.getAll().mapNotNull { reminder ->
            val experimentSyncId = experimentSyncIds[reminder.experimentId] ?: return@mapNotNull null
            ExperimentReminderSyncRow(
                syncId = reminder.syncId,
                experimentSyncId = experimentSyncId,
                text = reminder.text,
                reminderDateTime = reminder.reminderDateTime,
                updatedAt = reminder.updatedAt,
                deletedAt = reminder.deletedAt,
            )
        }

        return ExperimentSyncSnapshot(
            experiments = experiments.map {
                ExperimentSyncRow(
                    syncId = it.syncId,
                    title = it.title,
                    ideaDescription = it.ideaDescription,
                    isArchived = it.isArchived,
                    updatedAt = it.updatedAt,
                    deletedAt = it.deletedAt,
                )
            },
            entries = entries,
            reminders = reminders,
        )
    }

    private fun newSyncId(): String = UUID.randomUUID().toString()
}

private const val LEGACY_SEED_EXPERIMENT_TITLE = "New experiment"
private const val LEGACY_SEED_EXPERIMENT_DESCRIPTION =
    "Check a hypothesis and keep a journal of changes."
private const val LEGACY_SEED_ENTRY_CONTENT = "Initial experiment note."
