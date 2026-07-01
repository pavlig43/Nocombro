package ru.pavlig43.nocombro.mobile.experiments.internal.data

import androidx.room.withTransaction
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperiment
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntry
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentReminder
import ru.pavlig43.nocombro.mobile.internal.database.NocombroMobileDatabase
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentEntryEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.MobileExperimentReminderEntity
import ru.pavlig43.nocombro.mobile.internal.database.entity.toModel

class ExperimentDetailsRepository(
    private val db: NocombroMobileDatabase,
) {
    private val experimentDao = db.experimentDao
    private val entryDao = db.experimentEntryDao
    private val reminderDao = db.experimentReminderDao

    fun observeExperiment(id: Int): Flow<MobileExperiment?> {
        return experimentDao.observeExperiment(id)
            .map { experiment -> experiment?.toModel() }
    }

    fun observeEntries(experimentId: Int): Flow<List<MobileExperimentEntry>> {
        return entryDao.observeEntries(experimentId)
            .map { entries -> entries.map(MobileExperimentEntryEntity::toModel) }
    }

    fun observeReminders(experimentId: Int): Flow<List<MobileExperimentReminder>> {
        return reminderDao.observeReminders(experimentId)
            .map { reminders -> reminders.map(MobileExperimentReminderEntity::toModel) }
    }

    fun observeEntry(entryId: Int): Flow<MobileExperimentEntry> {
        return entryDao.observeEntry(entryId)
            .filterNotNull()
            .map { entry -> entry.toModel() }
    }

    suspend fun updateExperimentDraft(
        experimentId: Int,
        title: String,
        ideaDescription: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    title = title,
                    ideaDescription = ideaDescription,
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
        }
    }

    suspend fun setExperimentArchived(
        experimentId: Int,
        isArchived: Boolean,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val experiment = requireExperiment(experimentId)
            experimentDao.upsert(
                experiment.copy(
                    isArchived = isArchived,
                    updatedAt = getCurrentLocalDateTime(),
                )
            )
        }
    }

    suspend fun createEntryForDate(
        experimentId: Int,
        entryDate: LocalDate,
    ): Result<MobileExperimentEntry> = runCatching {
        db.withTransaction {
            val now = getCurrentLocalDateTime()
            val entry = MobileExperimentEntryEntity(
                experimentId = experimentId,
                entryDate = entryDate,
                createdAt = now,
                syncId = UUID.randomUUID().toString(),
                updatedAt = now,
            )
            val id = entryDao.create(entry).toInt()
            touchExperiment(experimentId, now)
            entry.copy(id = id).toModel()
        }
    }

    suspend fun updateEntryContent(
        entryId: Int,
        content: String,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val entry = requireNotNull(entryDao.getEntry(entryId)) {
                "Entry $entryId not found"
            }
            val now = getCurrentLocalDateTime()
            entryDao.upsert(
                entry.copy(
                    content = content,
                    updatedAt = now,
                )
            )
            touchExperiment(entry.experimentId, now)
        }
    }

    suspend fun createReminder(
        experimentId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<MobileExperimentReminder> = runCatching {
        db.withTransaction {
            val now = getCurrentLocalDateTime()
            val reminder = MobileExperimentReminderEntity(
                experimentId = experimentId,
                text = text,
                reminderDateTime = reminderDateTime,
                syncId = UUID.randomUUID().toString(),
                updatedAt = now,
            )
            val id = reminderDao.create(reminder).toInt()
            touchExperiment(experimentId, now)
            reminder.copy(id = id).toModel()
        }
    }

    suspend fun updateReminder(
        reminderId: Int,
        text: String,
        reminderDateTime: LocalDateTime,
    ): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = getCurrentLocalDateTime()
            reminderDao.upsert(
                reminder.copy(
                    text = text,
                    reminderDateTime = reminderDateTime,
                    updatedAt = now,
                )
            )
            touchExperiment(reminder.experimentId, now)
        }
    }

    suspend fun deleteReminder(reminderId: Int): Result<Unit> = runCatching {
        db.withTransaction {
            val reminder = requireNotNull(reminderDao.getReminder(reminderId)) {
                "Reminder $reminderId not found"
            }
            val now = getCurrentLocalDateTime()
            reminderDao.upsert(
                reminder.copy(
                    updatedAt = now,
                    deletedAt = now,
                )
            )
            touchExperiment(reminder.experimentId, now)
        }
    }

    private suspend fun requireExperiment(id: Int): MobileExperimentEntity {
        return requireNotNull(experimentDao.getExperiment(id)) {
            "Experiment $id not found"
        }
    }

    private suspend fun touchExperiment(
        experimentId: Int,
        updatedAt: LocalDateTime,
    ) {
        val experiment = requireExperiment(experimentId)
        experimentDao.upsert(experiment.copy(updatedAt = updatedAt))
    }
}
