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

    suspend fun deleteExperiment(
        experimentId: Int,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val deletedAt = defaultUpdatedAt()
                val experiment = requireNotNull(experimentDao.getExperiment(experimentId)) {
                    "Experiment $experimentId not found"
                }
                val entries = experimentEntryDao.getEntriesByExperiment(experimentId)
                val reminders = experimentReminderDao.getRemindersByExperiment(experimentId)
                val files = entries.flatMap { entry ->
                    fileDao.getFiles(entry.id, OwnerType.EXPERIMENT_ENTRY)
                }

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
                updated
            }
        }
    }

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
                touchExperiment(experimentId)
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
                touchExperiment(entry.experimentId)
            }
        }
    }

    suspend fun deleteEntry(
        entry: ExperimentEntry,
    ): Result<Unit> {
        return runCatching {
            database.inTransaction {
                val deletedAt = defaultUpdatedAt()
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
                touchExperiment(entry.experimentId)
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
            }
        }
    }

    private suspend fun touchExperiment(experimentId: Int) {
        val experiment = requireNotNull(experimentDao.getExperiment(experimentId)) {
            "Experiment $experimentId not found"
        }
        val updated = experiment.copy(updatedAt = defaultUpdatedAt())
        experimentDao.upsert(updated)
    }
}
