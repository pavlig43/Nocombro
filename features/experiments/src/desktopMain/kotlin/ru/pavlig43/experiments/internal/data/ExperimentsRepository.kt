package ru.pavlig43.experiments.internal.data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.experiment.EXPERIMENT_ENTRY_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_REMINDER_TABLE_NAME
import ru.pavlig43.database.data.experiment.EXPERIMENT_TABLE_NAME
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction

internal class ExperimentsRepository(
    db: NocombroDatabase,
    private val syncQueueRepository: SyncQueueRepository,
) {
    private val database = db
    private val experimentDao = db.experimentDao
    private val experimentEntryDao = db.experimentEntryDao
    private val experimentReminderDao = db.experimentReminderDao

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
                val experiment = Experiment(title = "Новый эксперимент")
                val id = experimentDao.create(experiment).toInt()
                val saved = experiment.copy(id = id)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_TABLE_NAME,
                    entityLocalId = saved.syncId,
                    createdAt = saved.updatedAt,
                )
                saved
            }
        }
    }

    suspend fun updateExperiment(
        experiment: Experiment,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                experimentDao.upsert(experiment)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_TABLE_NAME,
                    entityLocalId = experiment.syncId,
                    createdAt = experiment.updatedAt,
                )
            }
        }
    }

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
                    updatedAt = defaultUpdatedAt(),
                )
                experimentDao.upsert(updated)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_TABLE_NAME,
                    entityLocalId = updated.syncId,
                    createdAt = updated.updatedAt,
                )
                updated
            }
        }
    }

    suspend fun getOrCreateEntry(
        experimentId: Int,
        entryDate: LocalDate,
    ): Result<ExperimentEntry> {
        return runCatching {
            database.inTransaction {
                experimentEntryDao.getEntryByExperimentAndDate(experimentId, entryDate)
                    ?: run {
                        val entry = ExperimentEntry(
                            experimentId = experimentId,
                            entryDate = entryDate,
                        )
                        val id = experimentEntryDao.create(entry).toInt()
                        val saved = entry.copy(id = id)
                        touchExperiment(experimentId)
                        syncQueueRepository.enqueueUpsert(
                            entityTable = EXPERIMENT_ENTRY_TABLE_NAME,
                            entityLocalId = saved.syncId,
                            createdAt = saved.updatedAt,
                        )
                        saved
                    }
            }
        }
    }

    suspend fun updateEntry(
        entry: ExperimentEntry,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                experimentEntryDao.upsert(entry)
                touchExperiment(entry.experimentId)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_ENTRY_TABLE_NAME,
                    entityLocalId = entry.syncId,
                    createdAt = entry.updatedAt,
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
                touchExperiment(experimentId)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                    entityLocalId = saved.syncId,
                    createdAt = saved.updatedAt,
                )
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
                touchExperiment(reminder.experimentId)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                    entityLocalId = reminder.syncId,
                    createdAt = reminder.updatedAt,
                )
            }
        }
    }

    suspend fun deleteReminder(
        reminder: ExperimentReminder,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val updatedAt = defaultUpdatedAt()
                val deleted = reminder.copy(
                    updatedAt = updatedAt,
                    deletedAt = updatedAt,
                )
                experimentReminderDao.upsert(deleted)
                touchExperiment(reminder.experimentId)
                syncQueueRepository.enqueueUpsert(
                    entityTable = EXPERIMENT_REMINDER_TABLE_NAME,
                    entityLocalId = deleted.syncId,
                    createdAt = deleted.updatedAt,
                )
            }
        }
    }

    private suspend fun touchExperiment(experimentId: Int) {
        val experiment = requireNotNull(experimentDao.getExperiment(experimentId)) {
            "Experiment $experimentId not found"
        }
        val updated = experiment.copy(updatedAt = defaultUpdatedAt())
        experimentDao.upsert(updated)
        syncQueueRepository.enqueueUpsert(
            entityTable = EXPERIMENT_TABLE_NAME,
            entityLocalId = updated.syncId,
            createdAt = updated.updatedAt,
        )
    }
}
