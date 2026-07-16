package ru.pavlig43.experiments.internal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction

/**
 * Хранит дерево эксперимента и его tombstone в настольной Room-БД.
 *
 * Версии дочерних строк меняются независимо от родителя. Полное удаление выдаёт
 * всему дереву одну версию, строго более новую любой строки, и сохраняет файлы,
 * напоминания, записи и эксперимент как tombstone в одной транзакции.
 */
@Suppress("TooManyFunctions")
internal class ExperimentsRepository(
    db: NocombroDatabase,
) {
    private val database = db
    private val experimentDao = db.experimentDao
    private val experimentEntryDao = db.experimentEntryDao
    private val experimentReminderDao = db.experimentReminderDao
    private val fileDao = db.fileDao

    fun observeExperiments(
        isArchived: Boolean,
    ): Flow<List<Experiment>> = experimentDao.observeExperiments(isArchived)

    fun observeExperiment(
        id: Int,
    ): Flow<Experiment?> = experimentDao.observeExperiment(id)

    fun observeEntries(
        experimentId: Int,
    ): Flow<List<ExperimentEntry>> = experimentEntryDao.observeEntries(experimentId)

    fun observeEntry(
        entryId: Int,
    ): Flow<ExperimentEntry?> = experimentEntryDao.observeEntry(entryId)

    fun observeReminders(
        experimentId: Int,
    ): Flow<List<ExperimentReminder>> = experimentReminderDao.observeReminders(experimentId)

    suspend fun createExperiment(): Result<Experiment> {
        return runCatching {
            database.inTransaction {
                val experiment = Experiment(title = "")
                val id = experimentDao.create(experiment).toInt()
                experiment.copy(id = id)
            }
        }
    }

    suspend fun updateExperiment(
        experiment: Experiment,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                experimentDao.upsert(experiment)
            }
        }
    }

    /**
     * Помечает tombstone эксперимент и всех его потомков.
     *
     * @param experimentId локальный идентификатор корневой строки.
     * @return успех либо ошибка чтения или записи любой части дерева.
     */
    suspend fun deleteExperiment(
        experimentId: Int,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val experiment = requireNotNull(experimentDao.getExperiment(experimentId)) {
                    "Experiment $experimentId not found"
                }
                val entries = experimentEntryDao.getEntriesByExperiment(experimentId)
                val reminders = experimentReminderDao.getRemindersByExperiment(experimentId)
                val files = entries.flatMap { entry ->
                    fileDao.getFiles(entry.id, OwnerType.EXPERIMENT_ENTRY)
                }
                val previousVersion = buildList {
                    add(experiment.updatedAt)
                    experiment.deletedAt?.let(::add)
                    entries.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                    reminders.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                    files.forEach { add(it.deletedAt?.takeIf { deleted -> deleted > it.updatedAt } ?: it.updatedAt) }
                }.maxOrNull()
                val deletedAt = defaultUpdatedAt(previousVersion)

                if (files.isNotEmpty()) {
                    fileDao.upsertFiles(
                        files.map { file ->
                            file.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                        }
                    )
                }
                reminders.forEach { reminder ->
                    experimentReminderDao.upsert(
                        reminder.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                    )
                }
                entries.forEach { entry ->
                    experimentEntryDao.upsert(
                        entry.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                    )
                }
                experimentDao.upsert(
                    experiment.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                )
            }
        }
    }

    /**
     * Меняет признак архива с версией строго новее текущей строки.
     */
    suspend fun setExperimentArchived(
        experimentId: Int,
        isArchived: Boolean,
    ): Result<Experiment> {
        return runCatching {
            database.inTransaction {
                val experiment = requireNotNull(experimentDao.getExperiment(experimentId)) {
                    "Experiment $experimentId not found"
                }
                val updated = experiment.copy(
                    isArchived = isArchived,
                    updatedAt = defaultUpdatedAt(experiment.updatedAt),
                )
                experimentDao.upsert(updated)
                updated
            }
        }
    }

    /**
     * Создаёт запись журнала, не меняя sync-версию эксперимента.
     *
     * Последняя активность списка считается SQL-запросом по дочерним версиям.
     */
    suspend fun createEntryForDate(
        experimentId: Int,
        entryDate: LocalDate,
    ): Result<ExperimentEntry> {
        return runCatching {
            database.inTransaction {
                val entry = ExperimentEntry(
                    experimentId = experimentId,
                    entryDate = entryDate,
                )
                val id = experimentEntryDao.create(entry).toInt()
                val saved = entry.copy(id = id)
                saved
            }
        }
    }

    suspend fun updateEntry(
        entry: ExperimentEntry,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                experimentEntryDao.upsert(entry)
            }
        }
    }

    /**
     * Помечает запись и её файлы tombstone с версией новее самой записи.
     *
     * Версия эксперимента не меняется; все правки остаются независимыми mirror-строками.
     */
    suspend fun deleteEntry(
        entry: ExperimentEntry,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val deletedAt = defaultUpdatedAt(entry.deletedAt?.takeIf { it > entry.updatedAt } ?: entry.updatedAt)
                val files = fileDao.getFiles(entry.id, OwnerType.EXPERIMENT_ENTRY)
                if (files.isNotEmpty()) {
                    fileDao.upsertFiles(
                        files.map { file ->
                            file.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                        }
                    )
                }
                experimentEntryDao.upsert(
                    entry.copy(updatedAt = deletedAt, deletedAt = deletedAt)
                )
            }
        }
    }

    suspend fun createReminder(
        experimentId: Int,
        text: String,
        reminderDateTime: kotlinx.datetime.LocalDateTime,
    ): Result<ExperimentReminder> {
        return runCatching {
            database.inTransaction {
                val reminder = ExperimentReminder(
                    experimentId = experimentId,
                    text = text,
                    reminderDateTime = reminderDateTime,
                )
                val id = experimentReminderDao.create(reminder).toInt()
                val saved = reminder.copy(id = id)
                saved
            }
        }
    }

    suspend fun updateReminder(
        reminder: ExperimentReminder,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                experimentReminderDao.upsert(reminder)
            }
        }
    }

    /** Помечает напоминание tombstone с монотонно растущей версией. */
    suspend fun deleteReminder(
        reminder: ExperimentReminder,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val updatedAt = defaultUpdatedAt(
                    reminder.deletedAt?.takeIf { it > reminder.updatedAt } ?: reminder.updatedAt
                )
                val deleted = reminder.copy(
                    updatedAt = updatedAt,
                    deletedAt = updatedAt,
                )
                experimentReminderDao.upsert(deleted)
            }
        }
    }
}
